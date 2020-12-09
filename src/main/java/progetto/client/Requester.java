package progetto.client;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.util.Duration;
import progetto.common.Mail;
import progetto.common.Request;
import progetto.common.Response;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.util.List;
import java.util.NoSuchElementException;

public class Requester {
    private final static int MAX_TRIES = 10;
    private static final String connectionError = "Reconnection try number: ";

    private String host;
    private int port;
    private Mailbox mailbox;

    private Socket server;
    private ObjectOutputStream toServer;
    private ObjectInputStream fromServer;

    private String loggedAddress;

    private int tries = 0;
    private boolean failedMailList = false;
    private Alert reconnectionAlert = null;

    private SimpleStringProperty message = new SimpleStringProperty();
    private ObjectProperty<Alert.AlertType> alertType = new SimpleObjectProperty<>(Alert.AlertType.INFORMATION);

    private GetUpdatedMailList updateMailListTask = null;

    public Requester(String host, int port, Mailbox mailbox){
        this.host = host;
        this.port = port;
        this.mailbox = mailbox;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void getAndUpdateMailList(String givenMailAddress, ListView<Mail> mailListView, Button newBtn) {
        GetFullMailList getFullMailList = new GetFullMailList();
        System.out.println("Current: " + (updateMailListTask == null));

        getFullMailList.setOnFailed(new EventHandler<>() {
            @Override
            public void handle(WorkerStateEvent workerStateEvent) {
                Throwable exc = getFullMailList.getException();

                if (exc instanceof ConnectException) {

                    tries++;

                    message.set(connectionError + tries);

                    if (tries == 1) {
                        reconnectionAlert = CustomAlert.informationAlert("Connection error");
                        reconnectionAlert.contentTextProperty().bind(message);
                        reconnectionAlert.alertTypeProperty().bind(alertType);
                        reconnectionAlert.show();
                    }

                    System.out.println("Fallito");

                    if (tries == MAX_TRIES) {
                        alertType.setValue(Alert.AlertType.ERROR);
                        message.set("The server is currently down: try again later!");
                        reconnectionAlert.hide();
                    } else {
                        getFullMailList.restart();
                    }
                } else if(exc instanceof NoSuchElementException){
                    CustomAlert.wrongAddress(givenMailAddress).show();
                } else {
                    CustomAlert.internalError().show();
                }
            }
        });

        getFullMailList.setOnSucceeded(new EventHandler<>() {
            @Override
            public void handle(WorkerStateEvent workerStateEvent) {

                List<Mail> result = getFullMailList.getValue();
                if (result != null) {
                    for (Mail m : result) {
                        System.out.println(m);
                    }
                    mailbox.setAddress(givenMailAddress);
                    loggedAddress = givenMailAddress;
                    mailbox.setCurrentMailList(FXCollections.observableArrayList(result));
                    mailListView.setItems(mailbox.currentMailListProperty());// REMEMBER: seItems looks for changes in
                                                                             // the list, NOT if the list itself changes
                }

                if(reconnectionAlert != null)
                    reconnectionAlert.close();

                newBtn.setDisable(false);

                if(updateMailListTask != null){
                    System.out.println("Chiudo il vecchio");
                    updateMailListTask.cancel();
                }

                getUpdatedMailList(loggedAddress, mailListView);
            }
        });

        getFullMailList.start();
    }

    private class GetFullMailList extends Service<List<Mail>> {

        @Override
        protected Task<List<Mail>> createTask() {
            return new Task<>() {
                @Override
                protected List<Mail> call() throws Exception {
                    newConnectionAndStreams();
                    toServer.writeObject(new Request(Request.GET_FULL_MAILLIST, loggedAddress));
                    List<Mail> mailList = handleResponse(fromServer.readObject());

                    toServer.close();
                    fromServer.close();
                    server.close();

                    return mailList;
                }
            };
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void getUpdatedMailList(String givenMailAddress, ListView<Mail> mailListView) {
        GetUpdatedMailList getUpdatedMailList = new GetUpdatedMailList(givenMailAddress);

        tries = 0;

        getUpdatedMailList.setPeriod(Duration.seconds(3));
        getUpdatedMailList.setDelay(Duration.seconds(3));

        getUpdatedMailList.setOnFailed(new EventHandler<>() {
            @Override
            public void handle(WorkerStateEvent workerStateEvent) {
                Throwable exc = getUpdatedMailList.getException();

                failedMailList = true;

                if (exc instanceof ConnectException) {

                    tries++;
                    message.set(connectionError + tries);

                    if (tries == 1) {
                        reconnectionAlert = CustomAlert.informationAlert("Connection error");
                        reconnectionAlert.contentTextProperty().bind(message);
                        reconnectionAlert.alertTypeProperty().bind(alertType);
                        reconnectionAlert.show();
                    }

                    System.out.println("Fallito");

                    if (tries == MAX_TRIES) {
                        alertType.setValue(Alert.AlertType.ERROR);
                        message.set("The server is currently down: you can continue offline to look at your emails");
                        reconnectionAlert.hide();

                        getUpdatedMailList.cancel();
                    }
                } else if(exc instanceof NoSuchElementException){
                    CustomAlert.wrongAddress(givenMailAddress).show();
                } else {
                    CustomAlert.internalError().show();
                }
            }
        });

        getUpdatedMailList.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent workerStateEvent) {
                List<Mail> result = getUpdatedMailList.getLastValue();
                if(result != null && !result.isEmpty()){
                    if(failedMailList){
                        System.out.println("RECOVER FROM FAILURE");
                        mailbox.setCurrentMailList(result);
                        mailListView.setItems(mailbox.currentMailListProperty());

                        failedMailList = false;
                    } else{
                        System.out.println("Regolare");
                        mailbox.currentMailListProperty().addAll(result);
                    }
                }

                if(reconnectionAlert != null)
                    reconnectionAlert.close();
            }
        });

        getUpdatedMailList.start();
    }

    private class GetUpdatedMailList extends ScheduledService<List<Mail>> {
        private final String givenMailAddress;

        private GetUpdatedMailList(String givenMailAddress){
            this.givenMailAddress = givenMailAddress;
            updateMailListTask = this;
        }

        @Override
        protected Task<List<Mail>> createTask() {
            return new Task<>() {
                @Override
                protected List<Mail> call() throws IOException, ClassNotFoundException, NoSuchElementException, InternalError {
                    try{
                        newConnectionAndStreams();
                        toServer.writeObject(new Request(Request.UPDATE_MAILLIST, givenMailAddress));
                        return handleResponse(fromServer.readObject());
                    } finally {
                        toServer.close();
                        fromServer.close();
                        server.close();
                    }
                }
            };
        }
    }

    private void newConnectionAndStreams() throws IOException {

        System.out.println("Provo la connessione...");

        server = new Socket(host, port);
        toServer = new ObjectOutputStream(server.getOutputStream());
        fromServer = new ObjectInputStream(server.getInputStream());
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void deleteMail(){
        if(loggedAddress == null){

        }

        DeleteMail deleteMail = new DeleteMail();

        deleteMail.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent workerStateEvent) {
                mailbox.removeCurrentMail();
            }
        });

        deleteMail.setOnFailed(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent workerStateEvent) {

            }
        });
    }

    private class DeleteMail extends Task<Void>{


        @Override
        protected Void call() throws Exception {
            return null;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static List<Mail> handleResponse(Object o) throws NoSuchElementException, InternalError{
        if(!(o instanceof Response)) throw new InternalError();
        Response r = (Response) o;
        //System.out.println(r.getCode());
        switch (r.getCode()){
            case Response.OK:{
                /*List<Mail> a = r.getBody();
                System.out.println("AAAAA: " + a.isEmpty());
                for(Mail m : a){
                    System.out.println("HandleResponse ID: " + m.getID());
                }*/

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
