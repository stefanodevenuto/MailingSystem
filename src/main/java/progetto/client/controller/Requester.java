package progetto.client.controller;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;

import progetto.client.model.Mailbox;
import progetto.common.Mail;
import progetto.common.Request;
import progetto.common.Response;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Requester {
    private final static int MAX_TRIES = 10;
    private static final String connectionError = "Reconnection try number: ";
    public static final Pattern EMAIL_ADDRESS_REGEX =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);

    private final String host;
    private final int port;
    private final Mailbox mailbox;

    private Socket server;
    private ObjectOutputStream toServer;
    private ObjectInputStream fromServer;

    private int tries = 0;
    private boolean failedMailList = false;
    private Alert reconnectionAlert = null;

    private final SimpleStringProperty message = new SimpleStringProperty();
    private final ObjectProperty<Alert.AlertType> alertType = new SimpleObjectProperty<>(Alert.AlertType.INFORMATION);

    private GetUpdatedMailList updateMailListTask = null;

    public Requester(String host, int port, Mailbox mailbox){
        this.host = host;
        this.port = port;
        this.mailbox = mailbox;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Set (including the logged address) and progressively update the MailList contained in the Model
     * @param givenMailAddress the mail address inserted by the user
     * @param mailListView the ListView where the result will be visualized
     * @param newBtn the button used to create a new email, in order to make it usable in case of success
     */
    public void getAndUpdateMailList(String givenMailAddress, ListView<Mail> mailListView, Button newBtn) {
        GetFullMailList getFullMailList = new GetFullMailList(givenMailAddress);
        System.out.println("Current: " + (updateMailListTask == null));
        tries = 0;
        alertType.setValue(Alert.AlertType.INFORMATION);

        if(!validate(givenMailAddress)) {
            Alert alert = informationAlert("Wrong email");
            alert.setContentText("Please insert a valid mail address!");

            alert.showAndWait();
            return;
        }

        getFullMailList.setOnFailed(workerStateEvent -> {
            if(updateMailListTask != null){
                updateMailListTask.cancel();
            }

            Throwable exc = getFullMailList.getException();

            if (exc instanceof IOException) {
                tries++;
                message.set(connectionError + tries);

                // Create and show a reconnection alert with the try number
                if (tries == 1) {
                    reconnectionAlert = informationAlert("Connection error");
                    reconnectionAlert.contentTextProperty().bind(message);
                    reconnectionAlert.alertTypeProperty().bind(alertType);
                    reconnectionAlert.show();
                }

                // Change the alert to ERROR
                if (tries == MAX_TRIES) {
                    alertType.setValue(Alert.AlertType.ERROR);
                    message.set("The server is currently down: try again later!");
                } else {
                    getFullMailList.restart();
                }
            } else if(exc instanceof AddressNotFound){
                wrongAddress(((AddressNotFound) exc).getAddress()).show();
            } else {
                internalError().show();
            }
        });

        // Set the entire mail list to the list view
        getFullMailList.setOnSucceeded(workerStateEvent -> {

            List<Mail> result = getFullMailList.getValue();
            if (result != null) {
                for (Mail m : result) {
                    System.out.println(m);
                }
                mailbox.setAddress(givenMailAddress);
                mailbox.setCurrentMailList(result);
                mailListView.setItems(mailbox.currentMailListProperty());// REMEMBER: seItems looks for changes in
                                                                         // the list, NOT if the list itself changes
            }

            // Close the alert if a fail occurred
            if(reconnectionAlert != null)
                reconnectionAlert.close();

            newBtn.setDisable(false);

            // Cancel the previous update MailList task if re-logged
            if(updateMailListTask != null){
                updateMailListTask.cancel();
            }

            // Start the new update mail list service
            getUpdatedMailList(mailListView);
        });

        getFullMailList.start();
    }

    // Request the full mail list and handle the response
    private class GetFullMailList extends Service<List<Mail>> {
        private final String givenEmailAddress;

        private GetFullMailList(String givenEmailAddress){
            this.givenEmailAddress = givenEmailAddress;
        }

        @Override
        protected Task<List<Mail>> createTask() {
            return new Task<>() {
                @Override
                protected List<Mail> call() throws IOException, ClassNotFoundException, AddressNotFound, InternalError {
                    try{
                        newConnectionAndStreams();
                        toServer.writeObject(new Request(Request.GET_FULL_MAILLIST,
                                                         givenEmailAddress));
                        return handleResponse(fromServer.readObject());
                    } finally {
                        closeAll();
                    }
                }
            };
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Periodically update the current mail list
     * @param mailListView the ListView where the result will be visualized
     */
    private void getUpdatedMailList(ListView<Mail> mailListView) {
        GetUpdatedMailList getUpdatedMailList = new GetUpdatedMailList();

        tries = 0;
        SimpleBooleanProperty running = new SimpleBooleanProperty(true);
        alertType.setValue(Alert.AlertType.INFORMATION);

        getUpdatedMailList.setPeriod(Duration.seconds(3));
        getUpdatedMailList.setDelay(Duration.seconds(3));

        getUpdatedMailList.restartOnFailureProperty().bind(running);

        getUpdatedMailList.setOnFailed(workerStateEvent -> {
            Throwable exc = getUpdatedMailList.getException();

            failedMailList = true;

            if (exc instanceof IOException) {

                tries++;
                message.set(connectionError + tries);

                if (tries == 1) {
                    reconnectionAlert = informationAlert("Connection error");
                    reconnectionAlert.contentTextProperty().bind(message);
                    reconnectionAlert.alertTypeProperty().bind(alertType);
                    reconnectionAlert.show();
                }

                if (tries == MAX_TRIES) {
                    alertType.setValue(Alert.AlertType.ERROR);
                    message.set("The server is currently down: you can continue offline to look at your emails");

                    //getUpdatedMailList.cancel();
                    running.set(false);

                }
            } else if(exc instanceof AddressNotFound){
                wrongAddress(((AddressNotFound) exc).getAddress()).show();
            } else {
                exc.printStackTrace();
                internalError().show();
            }
        });

        getUpdatedMailList.setOnSucceeded(workerStateEvent -> {
            tries = 0;
            List<Mail> result = getUpdatedMailList.getLastValue();
            if (result != null && !result.isEmpty()) {
                if (failedMailList) {
                    System.out.println("RECOVER FROM FAILURE");
                    mailbox.setCurrentMailList(result);
                    mailListView.setItems(mailbox.currentMailListProperty());
                    mailListView.scrollTo(mailListView.getItems().size() - 1);

                    failedMailList = false;
                } else {
                    for(Mail m: result){
                        m.setNewMail(true);
                        mailbox.addCurrentMailList(m);
                    }

                    mailListView.scrollTo(mailListView.getItems().size() - 1);
                }
            }

            if (reconnectionAlert != null)
                reconnectionAlert.close();
        });

        getUpdatedMailList.start();
    }

    private class GetUpdatedMailList extends ScheduledService<List<Mail>> {

        private GetUpdatedMailList(){
            updateMailListTask = this;
        }

        @Override
        protected Task<List<Mail>> createTask() {
            return new Task<>() {
                @Override
                protected List<Mail> call() throws IOException, ClassNotFoundException, AddressNotFound, InternalError {
                    try{
                        newConnectionAndStreams();
                        toServer.writeObject(new Request(Request.UPDATE_MAILLIST,
                                                         mailbox.getAddress()));
                        return handleResponse(fromServer.readObject());
                    } finally {
                        closeAll();
                    }
                }
            };
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void deleteCurrentMail(SingleMailController singleMailController) {

        DeleteCurrentMail deleteCurrentMail = new DeleteCurrentMail();

        deleteCurrentMail.setOnSucceeded(workerStateEvent -> {
            mailbox.removeCurrentMail();
            singleMailController.hide();;
        });

        deleteCurrentMail.setOnFailed(workerStateEvent -> {
            Throwable exc = deleteCurrentMail.getException();

            if (exc instanceof IOException) {
                Alert notConnected = errorAlert("Not connected");
                notConnected.setContentText("The client is not connected: wait the reconnection process or " +
                        "redo the login process");
                notConnected.show();
            } else if (exc instanceof AddressNotFound) {
                wrongAddress(((AddressNotFound) exc).getAddress()).show();
            } else {
                internalError().show();
            }
        });

        deleteCurrentMail.start();
    }

    private class DeleteCurrentMail extends Service<Void>{

        @Override
        protected Task<Void> createTask() {
            return new Task<>() {
                @Override
                protected Void call() throws IOException, ClassNotFoundException, InternalError, AddressNotFound {
                    try {
                        newConnectionAndStreams();
                        toServer.writeObject(new Request(Request.DELETE,
                                                         mailbox.getAddress(),
                                                         mailbox.getCurrentMail()));
                        handleResponse(fromServer.readObject());

                        return null;
                    } finally {
                        closeAll();
                    }
                }
            };
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void sendCurrentMail(NewMailController newMailController) {
        SendCurrentMail sendCurrentMail = new SendCurrentMail();

        if(mailbox.getCurrentMail().getRecipients().isEmpty() || mailbox.getCurrentMail().getTitle() == null){
            Alert alert = informationAlert("Wrong email");
            alert.setContentText("Mail addresses and Title can't be empty!");

            alert.showAndWait();
            return;
        }


        for(String recipient : mailbox.getCurrentMail().getRecipients()){
            if(!validate(recipient)) {
                Alert alert = informationAlert("Wrong email");
                alert.setContentText("Please insert valid mail addresses!");

                alert.showAndWait();
                return;
            }
        }

        sendCurrentMail.setOnSucceeded(workerStateEvent -> {
            Alert success = informationAlert("Success");
            success.setContentText("Mail sent successfully!");
            success.show();

            newMailController.hide();
        });

        sendCurrentMail.setOnFailed(workerStateEvent -> {
            Throwable exc = sendCurrentMail.getException();

            if (exc instanceof IOException) {
                Alert notConnected = errorAlert("Not connected");
                notConnected.setContentText("The client is not connected: wait the reconnection process or " +
                        "redo the login one");
                notConnected.show();
            } else if (exc instanceof AddressNotFound) {
                Alert wrongAddress = warningAlert("Address not found");
                wrongAddress.setContentText("The inserted mail address " + ((AddressNotFound) exc).getAddress() + " doesn't exist!\n" +
                        "The email was sent only to addresses before " + ((AddressNotFound) exc).getAddress());
                wrongAddress.show();
            } else {
                exc.printStackTrace();
                internalError().show();
            }
        });

        sendCurrentMail.start();
    }

    private class SendCurrentMail extends Service<Void>{

        @Override
        protected Task<Void> createTask() {
            return new Task<>() {
                @Override
                protected Void call() throws IOException, ClassNotFoundException, InternalError, AddressNotFound {
                    try {
                        newConnectionAndStreams();
                        toServer.writeObject(new Request(Request.SEND,
                                                         mailbox.getAddress(),
                                                         mailbox.getCurrentMail()));
                        handleResponse(fromServer.readObject());

                        return null;
                    } finally {
                        closeAll();
                    }
                }
            };
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void newConnectionAndStreams() throws IOException {
        server = new Socket(host, port);
        toServer = new ObjectOutputStream(server.getOutputStream());
        fromServer = new ObjectInputStream(server.getInputStream());
    }

    // Close all open connections/streams
    private void closeAll() throws IOException {
        if(server != null)
            server.close();
        if(toServer != null)
            toServer.close();
        if(fromServer != null)
            fromServer.close();
    }

    private static boolean validate(String email) {
        Matcher matcher = EMAIL_ADDRESS_REGEX.matcher(email);
        return matcher.find();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private List<Mail> handleResponse(Object o) throws InternalError, AddressNotFound {
        if(!(o instanceof Response)) throw new InternalError();
        Response r = (Response) o;
        switch (r.getCode()){
            case Response.OK:{
                return r.getBody();
            }

            case Response.ADDRESS_NOT_FOUND:{
                throw new AddressNotFound(r.getError());
            }

            default:
                throw new InternalError();
        }
    }

    // Custom exception class created to express the address not found event, combined with the address in question
    private static class AddressNotFound extends Exception {
        private final String address;

        private AddressNotFound(String address){
            super();
            this.address = address;
        }

        public String getAddress() {
            return address;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
     * A series of useful static methods that simplifies the generation of Alerts
     */
    private static Alert informationAlert(String title){
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);

        return alert;
    }

    private static Alert errorAlert(String title){
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);

        return alert;
    }

    private static Alert warningAlert(String title){
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);

        return alert;
    }

    private static Alert wrongAddress(String address){
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Wrong Address");
        alert.setHeaderText(null);
        alert.setContentText("The inserted " + address + " address is not present!");

        return alert;
    }

    private static Alert internalError(){
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Internal Error");
        alert.setHeaderText(null);
        alert.setContentText("There's an internal server error: try again later!");

        return alert;
    }
}
