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
    private final static int MAX_TRIES = 10;                            // Max reconnection tries
    private static final String connectionError = "Reconnection try number: ";
    public static final Pattern EMAIL_ADDRESS_REGEX =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
            // Mail address regex

    private final String host;                                          // Current host name / IP
    private final int port;                                             // Current port
    private final Mailbox mailbox;                                      // The model

    private Socket server;                                              // Connection with the server
    private ObjectOutputStream toServer;                                // Output stream to the server
    private ObjectInputStream fromServer;                               // Input stream from the server

    private int tries = 0;                                              // Current reconnection try
    private Alert reconnectionAlert = null;                             // Reconnection alert in case of failure

    private final AtomicInteger emailCounter =                          // Read mails counter
            new AtomicInteger(0);

    private final SimpleStringProperty message =                        // Message of the alert property
            new SimpleStringProperty();
    private final ObjectProperty<Alert.AlertType> alertType =           // Type of the alert property
            new SimpleObjectProperty<>(Alert.AlertType.INFORMATION);

    private GetMailList updateMailListTask = null;                      // Reference to the old mail list task
    private boolean firstRequest;                                       // Reveal if it's the first mail list request

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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
     * @param newBtn the button used to create a new email, in order to make it usable/unusable in case of success/fail
     * @param singleMailController used to hide the view in case of login failure
     * @param newMailController used to hide the view in case of login failure
     */
    public void getAndUpdateMailList(String givenMailAddress, ListView<Mail> mailListView, Button newBtn,
                                     SingleMailController singleMailController, NewMailController newMailController) {

        // Check if the inserted text is a properly formatted mail address
        if(!validate(givenMailAddress)) {
            Alert alert = informationAlert("Wrong email");
            alert.setContentText("Please insert a valid mail address!");

            alert.showAndWait();
            return;
        }

        // Cancel the previous update mail list task
        if(updateMailListTask != null){
            updateMailListTask.cancel();
        }

        GetMailList getMailList = new GetMailList(givenMailAddress);

        // Initialize and re-initialize every user oriented variable
        tries = 0;
        //emailCounter.set(0);
        mailbox.clearCurrentMailList();
        firstRequest = true;
        alertType.setValue(Alert.AlertType.INFORMATION);

        // Bind boolean property to stop execution problems comes
        SimpleBooleanProperty running = new SimpleBooleanProperty(true);
        getMailList.setPeriod(Duration.seconds(3));
        getMailList.restartOnFailureProperty().bind(running);

        getMailList.setOnFailed(workerStateEvent -> {

            // Disable the view if the first request (new login) gone wrong
            if(firstRequest){
                newBtn.setDisable(true);
                mailbox.clearCurrentMailList();
                singleMailController.hide();
                newMailController.hide();
            }

            Throwable exc = getMailList.getException();
            if (exc instanceof IOException) {
                tries++;
                message.set(connectionError + tries);

                // Create the new alert box and bind tries number and severity
                if (tries == 1) {
                    reconnectionAlert = informationAlert("Connection error");
                    reconnectionAlert.contentTextProperty().bind(message);
                    reconnectionAlert.alertTypeProperty().bind(alertType);
                    reconnectionAlert.show();
                }

                // Stop execution when the maximum tries number has been reached and change alert properties accordingly
                if (tries == MAX_TRIES) {
                    if(firstRequest)
                        message.set("The server is currently down: try again later!");
                    else
                        message.set("The server is currently down: you can continue offline to look at your emails");

                    alertType.setValue(Alert.AlertType.ERROR);
                    running.set(false);
                }
            } else if(exc instanceof AddressNotFound){
                // Stop the execution if the user inserted a mail address not present
                wrongAddress(((AddressNotFound) exc).getAddress()).show();
                running.set(false);
            } else {
                // Stop the execution if server internal errors arrive
                internalError().show();
                running.set(false);
            }
        });

        getMailList.setOnSucceeded(workerStateEvent -> {

            tries = 0;
            newBtn.setDisable(false);
            mailbox.setAddress(givenMailAddress);

            // Recover the result/mail list
            List<Mail> result = getMailList.getValue();


            /*if(firstRequest) { // Set the current mail list if it's the first mail list request sent
                firstRequest = false;
                if (result != null) {
                    //mailbox.setCurrentMailList(result);
                    for(Mail m : result)
                        mailbox.addCurrentMailList(m);
                    //mailListView.setItems(mailbox.currentMailListProperty());
                }
            } else { // Add to the existing one otherwise
                if (result != null) {
                    for (Mail m : result) {
                        m.setNewMail(true);
                        mailbox.addCurrentMailList(m);
                    }
                }
            }*/

            if(result != null){
                for (Mail m : result) {
                    m.setNewMail(!firstRequest);
                    mailbox.addCurrentMailList(m);
                }

                if(!result.isEmpty())
                    mailListView.scrollTo(mailListView.getItems().size() - 1);

                if(firstRequest){
                    firstRequest = false;
                }
            }

            // In order to move the window of received mails
            /*if(result != null){
                //emailCounter.getAndAdd(result.size());
                if(!result.isEmpty())
                    // To immediately scroll to the bottom
                    mailListView.scrollTo(mailListView.getItems().size() - 1);
            }*/


            // Close the reconnection alert if an error occurred previously
            if (reconnectionAlert != null)
                reconnectionAlert.close();
        });

        getMailList.start();
    }

    // Create a new connection, send the MAILLIST request and return the mail list
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
                                                         /*emailCounter.get()*/mailbox.getSizeCurrentMailList()));
                        List<Mail> a = handleResponse(fromServer.readObject());
                        for(Mail m: a){
                            System.out.println("In create task: " + m);
                        }
                        return a;
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
            // Decrement the window of current mails accordingly to the elimination of the mail on the server
            //emailCounter.decrementAndGet();

            mailbox.removeCurrentMail();
            singleMailController.hide();
        });

        deleteCurrentMail.setOnFailed(workerStateEvent -> {
            Throwable exc = deleteCurrentMail.getException();

            if (exc instanceof IOException) {
                // Show a not connected alert if the user try to delete an email when not connected
                Alert notConnected = errorAlert("Not connected");
                notConnected.setContentText("The client is not connected: wait the reconnection process or " +
                        "redo the login process");
                notConnected.show();
            } else if (exc instanceof AddressNotFound) {
                // Show the address not found alert if a not present address have been inserted
                wrongAddress(((AddressNotFound) exc).getAddress()).show();
            } else {
                internalError().show();
            }
        });

        deleteCurrentMail.start();
    }

    // Create a new connection and send the DELETE request
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

        // Check if Title and Recipients are valued
        if(mailbox.getCurrentMail().getRecipients().isEmpty() || mailbox.getCurrentMail().getTitle() == null){
            Alert alert = informationAlert("Wrong email");
            alert.setContentText("Mail addresses and Title can't be empty!");

            alert.showAndWait();
            return;
        }

        // Check if inserted strings are properly formatted mail addresses
        for(String recipient : mailbox.getCurrentMail().getRecipients()){
            if(!validate(recipient)) {
                Alert alert = informationAlert("Wrong email");
                alert.setContentText("Please insert valid mail addresses!");

                alert.showAndWait();
                return;
            }
        }

        sendCurrentMail.setOnSucceeded(workerStateEvent -> {
            // Show the mail sent alert
            Alert success = informationAlert("Success");
            success.setContentText("Mail sent successfully!");
            success.show();

            // Start the animation of sent mail
            newMailController.hide();
        });

        sendCurrentMail.setOnFailed(workerStateEvent -> {
            Throwable exc = sendCurrentMail.getException();

            if (exc instanceof IOException) {
                // Show a not connected alert if the user try to send an email when not connected
                Alert notConnected = errorAlert("Not connected");
                notConnected.setContentText("The client is not connected: wait the reconnection process or " +
                        "redo the login one");
                notConnected.show();
            } else if (exc instanceof AddressNotFound) {
                // Show the address not found alert, along with the indicted address,
                // if a not present mail addresses have been inserted
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

    // Create a new connection and send the SEND request
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

    // Create a new Socket along with his Streams
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

    /**
     * Handle the responses received by the server
     * @param rawResponse the given raw Response from server
     * @return the body of the Response
     * @throws InternalError when the server have problems
     * @throws AddressNotFound when one or more address were not found
     */
    private List<Mail> handleResponse(Object rawResponse) throws InternalError, AddressNotFound {
        if(!(rawResponse instanceof Response)) throw new InternalError();
        Response r = (Response) rawResponse;
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
