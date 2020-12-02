package progetto.client;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import progetto.common.Mail;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import progetto.common.Request;
import progetto.common.Response;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class LoginAndMailboxController {

    private Mailbox mailbox;
    private HashMap<String, Pane> screenMap;
    private BorderPane root;
    private ScheduledExecutorService executorService;
    public static final Pattern EMAIL_ADDRESS_REGEX =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);

    @FXML
    private TextField insertedMail;

    @FXML
    private ListView<Mail> mailListView;

    public void initController(Mailbox mailbox, HashMap<String, Pane> screenMap, BorderPane root, ScheduledExecutorService executorService) {
        // ensure model is only set once
        if (this.mailbox != null) {
            throw new IllegalStateException("Model can only be initialized once");
        }

        this.mailbox = mailbox;
        this.screenMap = screenMap;
        this.root = root;
        this.executorService = executorService;
    }

    @FXML
    public void handleLoginButton(ActionEvent actionEvent) {

        String givenMailAddress = insertedMail.getText();
        if(!executorService.isShutdown()){
            // TODO: gestire quando premo login una seconda volta l'interruzione del primo Runnable ogni 3 sec.
        }

        if(!validate(givenMailAddress)) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Wrong email");
            alert.setContentText("Please insert a valid email");

            alert.showAndWait();
            return;
        }

        try {
            Socket server = new Socket("localhost", 4444);

            try {
                ObjectOutputStream toServer = new ObjectOutputStream(server.getOutputStream());
                ObjectInputStream fromServer = new ObjectInputStream(server.getInputStream());

                toServer.writeObject(new Request(Request.GET_FULL_MAILLIST, givenMailAddress));
                System.out.println("Requested full maillist");
                Object o = fromServer.readObject();

                List<Mail> m;

                try {
                    m = handleResponse(o);
                } catch (NoSuchElementException noSuchElementException){
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Wrong email");
                    alert.setContentText("The inserted email doesn't exist!");

                    alert.showAndWait();
                    return;
                } catch (InternalError internalError){
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Internal error");
                    alert.setContentText("Internal error: Try again later!");

                    alert.showAndWait();
                    return;
                }


                // Retrieve the original ObservableList type
                ObservableList<Mail> mailList = FXCollections.observableArrayList(m);

                mailbox.setAddress(givenMailAddress);
                mailbox.setCurrentMailList(mailList);

            } finally {
                System.out.println("Chiuso");
                server.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Set the entire MailList (ObservableList<Mail>) to the ListView
        mailListView.setItems(mailbox.currentMailListProperty());

        // Show the title of the Mail for each one in the ObservableList
        mailListView.setCellFactory(lv -> new ListCell<Mail>() {
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

        mailListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                Mail a = (Mail) newValue;
                mailbox.setCurrentMail(a);

                //screenMap.get("singleMail").setVisible(true);
                root.setRight(screenMap.get("singleMail"));
            }
        });

        // Create a Runnable with a call to Platform.runLater
        Runnable getCurrentMailList = new GetCurrentMailList(givenMailAddress, mailListView);
        executorService.scheduleAtFixedRate(getCurrentMailList, 5, 5, TimeUnit.SECONDS);

    }

    public static boolean validate(String email) {
        Matcher matcher = EMAIL_ADDRESS_REGEX.matcher(email);
        return matcher.find();
    }

    private class GetCurrentMailList implements Runnable {
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
                    try {
                        Socket server = new Socket("localhost", 4444);

                        try {
                            ObjectOutputStream toServer = new ObjectOutputStream(server.getOutputStream());
                            ObjectInputStream fromServer = new ObjectInputStream(server.getInputStream());

                            toServer.writeObject(new Request(Request.UPDATE_MAILLIST, givenMailAddress));

                            Object o = fromServer.readObject();

                            List<Mail> m;

                            try {
                                m = handleResponse(o);
                            } catch (NoSuchElementException noSuchElementException){
                                Alert alert = new Alert(Alert.AlertType.WARNING);
                                alert.setTitle("Wrong email");
                                alert.setContentText("The inserted email doesn't exist!");

                                alert.showAndWait();
                                return;
                            } catch (InternalError internalError){
                                Alert alert = new Alert(Alert.AlertType.ERROR);
                                alert.setTitle("Internal error");
                                alert.setContentText("Internal error: Try again later!");

                                alert.showAndWait();
                                return;
                            }

                            ObservableList<Mail> mailList = FXCollections.observableArrayList(m);

                            mailListView.getItems().addAll(mailList);
                        } finally {
                            System.out.println("Chiuso update mailList");
                            server.close();
                        }
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private List<Mail> handleResponse(Object o) throws NoSuchElementException, InternalError{
        if(!(o != null && o instanceof Response)) throw new InternalError();
        Response r = (Response) o;
        System.out.println(r.getCode());
        switch (r.getCode()){
            case Response.OK:{
                return r.getBody();
            }

            case Response.ADDRESS_NOT_FOUND:{
                throw new NoSuchElementException();
            }

            default:
                throw new InternalError();
        }
    }
}
