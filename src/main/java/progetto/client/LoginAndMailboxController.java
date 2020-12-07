package progetto.client;

import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.value.ObservableListValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.*;
import javafx.event.EventHandler;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.util.Duration;
import progetto.common.Mail;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import progetto.common.Request;
import progetto.common.Response;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class LoginAndMailboxController {

    private Mailbox mailbox;
    private HashMap<String, Pane> screenMap;
    private BorderPane root;
    private ScheduledExecutorService executorService;
    private UpdateMailbox updateMailbox;

    public static final Pattern EMAIL_ADDRESS_REGEX =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
    public static final int MAX_TRIES = 10;

    @FXML
    private TextField insertedMail;

    @FXML
    private ListView<Mail> mailListView;

    @FXML
    private Button newBtn;

    public void initController(Mailbox mailbox, HashMap<String, Pane> screenMap, BorderPane root, ScheduledExecutorService executorService) {
        // ensure model is only set once
        if (this.mailbox != null) {
            throw new IllegalStateException("Model can only be initialized once");
        }

        this.mailbox = mailbox;
        this.screenMap = screenMap;
        this.root = root;
        this.executorService = executorService;
        this.updateMailbox = null;
    }

    @FXML
    public void handleNewButton(ActionEvent actionEvent) {
        Mail newMail = new Mail();

        mailbox.setCurrentMail(newMail);

        root.setRight(screenMap.get("newMail"));
    }

    @FXML
    public void handleLoginButton(ActionEvent actionEvent) {

        String givenMailAddress = insertedMail.getText();


        if(updateMailbox != null){
            // TODO: gestire quando premo login una seconda volta l'interruzione del primo Runnable ogni 3 sec.
            updateMailbox.cancel();
        }

        if(!validate(givenMailAddress)) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Wrong email");
            alert.setHeaderText(null);
            alert.setContentText("Please insert a valid email");

            alert.showAndWait();
            return;
        }
        boolean notConnected = true;
        int tries = 0;
        while(notConnected){
            try {
                Socket server = new Socket("localhost", 4444);
                notConnected = false;
                try {
                    ObjectOutputStream toServer = new ObjectOutputStream(server.getOutputStream());
                    ObjectInputStream fromServer = new ObjectInputStream(server.getInputStream());

                    try {
                        toServer.writeObject(new Request(Request.GET_FULL_MAILLIST, givenMailAddress));
                        System.out.println("Requested full maillist");
                        Object o = fromServer.readObject();

                        List<Mail> m;

                        try {
                            m = handleResponse(o);
                        } catch (NoSuchElementException noSuchElementException){
                            Alert alert = new Alert(Alert.AlertType.WARNING);
                            alert.setTitle("Wrong email");
                            alert.setHeaderText(null);
                            alert.setContentText("The inserted email doesn't exist!");

                            alert.showAndWait();
                            return;
                        } catch (InternalError internalError){
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Internal error");
                            alert.setHeaderText(null);
                            alert.setContentText("Internal error: Try again later!");

                            alert.showAndWait();
                            return;
                        }

                        // Retrieve the original ObservableList type
                        ObservableList<Mail> mailList = FXCollections.observableArrayList(m);

                        mailbox.setAddress(givenMailAddress);
                        mailbox.setCurrentMailList(mailList);
                    } finally {
                        toServer.close();
                        fromServer.close();
                    }


                } finally {
                    System.out.println("Chiuso");
                    server.close();
                }

            } catch (ConnectException e) {
                tries++;
                if(tries == MAX_TRIES){
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Internal error");
                    alert.setContentText("The server is currently down: try again later!");

                    alert.showAndWait();
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        for(Mail m : mailbox.getCurrentMailList()){
            System.out.println(m);
        }

        // Set the entire MailList (ObservableList<Mail>) to the ListView
        mailListView.setItems(mailbox.currentMailListProperty());
        //mailListView.setItems(listTry);

        // Show the title of the Mail for each one in the ObservableList
        mailListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            public void updateItem(Mail mail, boolean empty) {
                super.updateItem(mail, empty);
                if (empty) {
                    setText(null);
                } else {
                    setText("Title: " + mail.getTitle() + "\n" +
                            "From: " + mail.getSender() + "\n");
                }
            }
        });

        newBtn.setDisable(false);
        /*mailListView.getSelectionModel().selectedItemProperty().addListener((observable, oldMail, newMail) -> {
            mailbox.setCurrentMail(newMail);
            //screenMap.get("singleMail").setVisible(true);
            root.setRight(screenMap.get("singleMail"));
            System.out.println("Clicked");
        });*/

        // Create a Runnable with a call to Platform.runLater
        //Runnable getCurrentMailList = new GetCurrentMailList(givenMailAddress, mailListView);
        //timedGetMailList = executorService.scheduleAtFixedRate(getCurrentMailList, 5, 5, TimeUnit.SECONDS);

        updateMailbox = new UpdateMailbox(givenMailAddress, Request.UPDATE_MAILLIST);

        updateMailbox.setPeriod(Duration.seconds(3));
        updateMailbox.setDelay(Duration.seconds(3));

        updateMailbox.setRestartOnFailure(false);

        updateMailbox.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent workerStateEvent) {
                List<Mail> result = updateMailbox.getLastValue();
                if(result != null){
                    mailbox.currentMailListProperty().addAll(result);
                }
            }
        });

        updateMailbox.setOnFailed(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent workerStateEvent) {
                Throwable exc = updateMailbox.getException();

                if (exc instanceof NoSuchElementException) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Wrong email");
                    alert.setHeaderText(null);
                    alert.setContentText("The inserted email doesn't exist!");

                    alert.showAndWait();
                } else if (exc instanceof ConnectException) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Internal error");
                    alert.setHeaderText(null);
                    alert.setContentText("The server is currently down: try again later!");

                    alert.showAndWait();
                } else  {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Internal error");
                    alert.setHeaderText(null);
                    alert.setContentText("Internal error: Try again later!");

                    alert.showAndWait();
                }
            }
        });


        //mailListView.itemsProperty().bind(updateMailbox.lastValueProperty());
        //mailListView.getItems().addAll(updateMailbox.getLastValue());
        updateMailbox.start();
    }


    @FXML
    public void handleMouseClicked(MouseEvent arg0) {
        Mail m = mailListView.getSelectionModel().getSelectedItem();
        if(m != null){
            System.out.println("Clicked: " + m.getID() + " " + m.toString());
            mailbox.setCurrentMail(m);
            root.setRight(screenMap.get("singleMail"));
        }
    }

    private static boolean validate(String email) {
        Matcher matcher = EMAIL_ADDRESS_REGEX.matcher(email);
        return matcher.find();
    }

    /*private class GetCurrentMailList implements Runnable {
        private String givenMailAddress;
        private ListView<Mail> mailListView;

        private GetCurrentMailList(String givenMailAddress, ListView<Mail> mailListView) {
            this.givenMailAddress = givenMailAddress;
            this.mailListView = mailListView;
        }

        @Override
        public void run() {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    boolean notConnected = true;
                    int tries = 0;
                    while(notConnected) {
                        try {
                            Socket server = new Socket("localhost", 4444);
                            notConnected = false;
                            try {
                                ObjectOutputStream toServer = new ObjectOutputStream(server.getOutputStream());
                                ObjectInputStream fromServer = new ObjectInputStream(server.getInputStream());

                                toServer.writeObject(new Request(Request.UPDATE_MAILLIST, givenMailAddress));

                                Object o = fromServer.readObject();

                                List<Mail> m;

                                try {
                                    m = handleResponse(o);
                                } catch (NoSuchElementException noSuchElementException) {
                                    Alert alert = new Alert(Alert.AlertType.WARNING);
                                    alert.setTitle("Wrong email");
                                    alert.setHeaderText(null);
                                    alert.setContentText("The inserted email doesn't exist!");

                                    alert.showAndWait();
                                    return;
                                } catch (InternalError internalError) {
                                    Alert alert = new Alert(Alert.AlertType.ERROR);
                                    alert.setTitle("Internal error");
                                    alert.setHeaderText(null);
                                    alert.setContentText("Internal error: Try again later!");

                                    alert.showAndWait();
                                    return;
                                }

                                ObservableList<Mail> mailList = FXCollections.observableArrayList(m);

                                mailListView.getItems().addAll(mailList);
                            } finally {
                                //System.out.println("Chiuso update mailList");
                                server.close();
                            }
                        } catch (ConnectException e) {
                            tries++;
                            if (tries == MAX_TRIES) {
                                Alert alert = new Alert(Alert.AlertType.ERROR);
                                alert.setTitle("Internal error");
                                alert.setHeaderText(null);
                                alert.setContentText("The server is currently down: try again later!");

                                alert.showAndWait();
                                return;
                            }

                            if(tries == 1){

                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
    }*/

    private static List<Mail> handleResponse(Object o) throws NoSuchElementException, InternalError{
        if(!(o != null && o instanceof Response)) throw new InternalError();
        Response r = (Response) o;
        //System.out.println(r.getCode());
        switch (r.getCode()){
            case Response.OK:{
                List<Mail> a = r.getBody();
                System.out.println("AAAAA: " + a.isEmpty());
                for(Mail m : a){
                    System.out.println("HandleResponse ID: " + m.getID());
                }

                return r.getBody();
            }

            case Response.ADDRESS_NOT_FOUND:{
                throw new NoSuchElementException();
            }

            default:
                throw new InternalError();
        }
    }


    private class UpdateMailbox extends ScheduledService<List<Mail>> {
        private final String address;
        private int requestType;

        private UpdateMailbox(String address, int requestType){
            this.address = address;
            this.requestType = requestType;
        }

        @Override
        protected Task<List<Mail>> createTask() {
            return new UpdateCurrentMailList();
        }

        private class UpdateCurrentMailList extends Task<List<Mail>> {

            @Override
            protected List<Mail> call() throws Exception {

                boolean notConnected = true;
                int tries = 0;

                System.out.println("Timed requested by: " + address);

                ObservableList<Mail> mailList = null;
                List<Mail> m = null;
                while(notConnected) {
                    try {
                        Socket server = new Socket("localhost", 4444);
                        notConnected = false;
                        try {
                            ObjectOutputStream toServer = new ObjectOutputStream(server.getOutputStream());
                            ObjectInputStream fromServer = new ObjectInputStream(server.getInputStream());

                            toServer.writeObject(new Request(requestType, address));

                            requestType = Request.UPDATE_MAILLIST;

                            Object o = fromServer.readObject();

                            m = handleResponse(o);

                            mailbox.setAddress(address);

                            //mailListView.getItems().addAll(mailList);
                        } finally {
                            //System.out.println("Chiuso update mailList");
                            server.close();
                        }
                    } catch (ConnectException e) {
                        tries++;
                        if (tries == MAX_TRIES) {
                            throw new ConnectException();
                            //return;
                        }
                        System.out.println("Failed!");
                        if(tries == 1){

                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                /*System.out.println("Lista vuota? " + mailList.isEmpty());

                if(m != null){
                    mailbox.currentMailListProperty().addAll(m);
                }

                //current.add(new Mail());

                for(Mail b : current){
                    System.out.println("M: " + b);
                }*/

                return m;
            }

        }
    }
}
