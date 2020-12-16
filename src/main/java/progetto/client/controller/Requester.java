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
import java.util.concurrent.atomic.AtomicInteger;
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
    private Alert reconnectionAlert = null;

    private final AtomicInteger emailCounter = new AtomicInteger(0);

    private final SimpleStringProperty message = new SimpleStringProperty();
    private final ObjectProperty<Alert.AlertType> alertType = new SimpleObjectProperty<>(Alert.AlertType.INFORMATION);

    private GetMailList updateMailListTask = null;
    private boolean firstRequest;

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

        if(updateMailListTask != null){
            updateMailListTask.cancel();
        }

        // Check if the inserted text is a properly formatted mail address
        if(!validate(givenMailAddress)) {
            Alert alert = informationAlert("Wrong email");
            alert.setContentText("Please insert a valid mail address!");

            alert.showAndWait();
            return;
        }

        GetMailList getMailList = new GetMailList(givenMailAddress);

        // Initialize and re-initialize every user oriented variable
        tries = 0;
        emailCounter.set(0);
        firstRequest = true;

        SimpleBooleanProperty running = new SimpleBooleanProperty(true);
        getMailList.setPeriod(Duration.seconds(3));
        getMailList.restartOnFailureProperty().bind(running);

        alertType.setValue(Alert.AlertType.INFORMATION);

        getMailList.setOnFailed(workerStateEvent -> {
            Throwable exc = getMailList.getException();
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
                    running.set(false);
                }
            } else if(exc instanceof AddressNotFound){
                wrongAddress(((AddressNotFound) exc).getAddress()).show();
                running.set(false);
            } else {
                exc.printStackTrace();
                internalError().show();
                running.set(false);
            }
        });

        getMailList.setOnSucceeded(workerStateEvent -> {
            tries = 0;
            newBtn.setDisable(false);
            mailbox.setAddress(givenMailAddress);

            List<Mail> result = getMailList.getValue();

            if(firstRequest) {
                firstRequest = false;
                if (result != null) {
                    mailbox.setCurrentMailList(result);
                    mailListView.setItems(mailbox.currentMailListProperty());
                }
            } else {
                if (result != null) {
                    for (Mail m : result) {
                        m.setNewMail(true);
                        mailbox.addCurrentMailList(m);
                    }
                }
            }

            mailListView.scrollTo(mailListView.getItems().size() - 1);

            if(result != null)
                emailCounter.getAndAdd(result.size());

            if (reconnectionAlert != null)
                reconnectionAlert.close();
        });

        getMailList.start();
    }

    private class GetMailList extends ScheduledService<List<Mail>> {
        private final String givenAddress;

        private GetMailList(String givenAddress){
            updateMailListTask = this;
            this.givenAddress = givenAddress;
        }

        @Override
        protected Task<List<Mail>> createTask() {
            return new Task<>() {
                @Override
                protected List<Mail> call() throws IOException, ClassNotFoundException, AddressNotFound, InternalError {
                    try{
                        newConnectionAndStreams();
                        toServer.writeObject(new Request(Request.MAILLIST,
                                                         givenAddress,
                                                         emailCounter.get()));
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
     * Send a delete mail request and remove it from the mail list
     * @param singleMailController in order to call the hide function that make the delete animation possible
     */
    public void deleteCurrentMail(SingleMailController singleMailController) {

        DeleteCurrentMail deleteCurrentMail = new DeleteCurrentMail();

        deleteCurrentMail.setOnSucceeded(workerStateEvent -> {
            emailCounter.decrementAndGet();
            mailbox.removeCurrentMail();
            singleMailController.hide();
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

    /**
     * Send a send mail request and add it to the mail list
     * @param newMailController in order to call the hide function that make the send animation possible
     */
    public void sendCurrentMail(NewMailController newMailController) {
        SendCurrentMail sendCurrentMail = new SendCurrentMail();

        // Title and recipients have to be valued
        if(mailbox.getCurrentMail().getRecipients().isEmpty() || mailbox.getCurrentMail().getTitle() == null){
            Alert alert = informationAlert("Wrong email");
            alert.setContentText("Mail addresses and Title can't be empty!");

            alert.showAndWait();
            return;
        }

        // Inserted emails have to be real mail addresses
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
                String addressNotFound = ((AddressNotFound) exc).getAddress();

                Alert wrongAddress = warningAlert("Address not found");
                wrongAddress.setContentText("The inserted mail address " + addressNotFound + " doesn't exist!\n" +
                        "The email was sent only to addresses before " + addressNotFound);
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

    // Check if a given string is an email address properly formatted
    private static boolean validate(String email) {
        Matcher matcher = EMAIL_ADDRESS_REGEX.matcher(email);
        return matcher.find();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // Handle the responses received by the server
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
