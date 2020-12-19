package progetto.server;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import progetto.client.model.Mailbox;
import progetto.common.Mail;
import javafx.fxml.FXML;
import progetto.common.Request;
import progetto.common.Response;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;

public class ServerLogController {
    private Mailboxes mailboxes;                                    // The model
    private ExecutorService executors;                              // The shared executor service

    @FXML
    private TableView<Log> logsTable;                               // Table view that shows the logs

    @FXML
    private TableColumn<Log, String> dateColumn;                    // Date column of the table

    @FXML
    private TableColumn<Log, String> requesterColumn;               // Requester column of the table

    @FXML
    private TableColumn<Log, Request> requestColumn;                // Request column of the table

    @FXML
    private TableColumn<Log, Image> statusImageColumn;              // Status image column of the table

    @FXML
    private TableColumn<Log, String> statusTextColumn;              // Status text column of the table

    /**
     * Initialize the controller with proper information
     * @param mailboxes the model
     * @param executors the share executor service
     * @return the listener runnable
     */
    public StartListener initController(Mailboxes mailboxes, ExecutorService executors) {

        // ensure model is only set once
        if (this.mailboxes != null) {
            throw new IllegalStateException("Model can only be initialized once");
        }

        this.mailboxes = mailboxes;
        this.executors = executors;

        // Set the items in the logs table to the logs property in the Model
        logsTable.setItems(mailboxes.logsProperty());

        // Set the cell value factory form every column
        dateColumn.setCellValueFactory(date -> date.getValue().dateTimeProperty());
        requesterColumn.setCellValueFactory(requester -> requester.getValue().requesterProperty());
        requestColumn.setCellValueFactory(request -> request.getValue().requestProperty());

        // Set the value factory of the status image column in order to properly visualize the images
        statusImageColumn.setCellFactory(logImageViewTableColumn -> {
            final ImageView imageView = new ImageView();
            imageView.setFitHeight(24);
            imageView.setFitWidth(24);

            //Set up the Table cell
            TableCell<Log, Image> cell = new TableCell<>() {
                public void updateItem(Image item, boolean empty) {
                    imageView.setImage(item);
                }
            };
            // Attach the imageview to the cell
            cell.setGraphic(imageView);
            return cell;
        });

        statusImageColumn.setCellValueFactory(status -> status.getValue().statusProperty());
        statusTextColumn.setCellValueFactory(statusText -> statusText.getValue().statusTextProperty());

        // Create the listener and start it
        StartListener startListener = new StartListener();
        this.executors.execute(startListener);

        return startListener;
    }


    public class StartListener implements Runnable{

        private ServerSocket acceptor = null;       // Acceptor socket

        @Override
        public void run() {
            try {
                acceptor = new ServerSocket(4444);

                // Accepts new incoming requests
                while(true) {
                    Socket client = acceptor.accept();

                    // Create a new handler for every request and start it
                    Runnable requestsHandler = new RequestsHandler(client, executors);
                    executors.execute(requestsHandler);
                }
            } catch (IOException e) {
                // Socket is closed
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // CLose the acceptor socket in order to avoid new incoming requests
        public void closeSocket() throws IOException {
            if(acceptor != null)
                acceptor.close();
        }
    }

    // Handle the different types of requests
    private class RequestsHandler implements Runnable {
        private final Socket client;                            // Socket of the client
        private final ExecutorService executorService;          // The shared executor service

        private RequestsHandler(Socket client, ExecutorService executorService) {
            this.client = client;
            this.executorService = executorService;
        }

        @Override
        public void run() {
            try {

                // Instantiate the needed streams to/from the user
                ObjectInputStream fromClient = new ObjectInputStream(client.getInputStream());
                ObjectOutputStream toClient = new ObjectOutputStream(client.getOutputStream());

                // Read the incoming request
                Object request = fromClient.readObject();

                // Check if it's a properly formatted Request
                if(!(request instanceof Request)) {
                    System.out.println("Server: BAD REQUEST");
                    badRequestError(toClient);
                    return;
                }

                // Recover the request type
                Request r = (Request) request;

                // Create a new log that documents the current request
                Log log = new Log(r.getAddress(), r, Mailboxes.LOAD);

                // Create and execute a new Task base on the request type
                switch (r.getType()) {
                    case Request.SEND:{
                        executorService.execute(new WriteMail(toClient, r, log));
                        break;
                    }

                    case Request.DELETE:{
                        executorService.execute(new DeleteMail(toClient, r, log));
                        break;
                    }

                    case Request.MAILLIST:{
                        executorService.execute(new SendMailList(toClient, r, log));
                        break;
                    }

                }

                // Add the log to the list, on order to make it visible in the table
                mailboxes.logsProperty().add(log);

            } catch (IOException | ClassNotFoundException e) {
                //connectionError(r);
                e.printStackTrace();
            }
        }
    }

    // Recover the mail list and send it to the requester
    private class SendMailList implements Runnable {
        private final ObjectOutputStream toClient;              // The stream to the client
        private final Request request;                          // The came request
        private final Log log;                                  // The log associated

        private SendMailList(ObjectOutputStream toClient, Request request, Log log) {
            this.toClient = toClient;
            this.request = request;
            this.log = log;
        }

        @Override
        public void run() {
            try {
                // Recover the mail list
                List<Mail> mailList = mailboxes.getMailboxMailList(request.getAddress(), request.getCounter());

                // Send an affirmative response to client
                sendOK(toClient, request, mailList, log);
            } catch(NoSuchElementException noSuchElementException) {
                // If the given mail address is not present
                addressNotFoundError(toClient, request.getAddress(), request, log);
            } catch (Exception e){
                internalError(toClient, request, log);  // If something gone wrong
            }
        }
    }

    // Write a new mail
    private class WriteMail implements Runnable{
        private final ObjectOutputStream toClient;                  // The stream to the client
        private final Request request;                              // The came request
        private final Log log;                                      // The log associated

        private WriteMail(ObjectOutputStream toClient, Request request, Log log) {
            this.toClient = toClient;
            this.request = request;
            this.log = log;
        }

        @Override
        public void run() {
            String lastAddress = null;
            try {

                // Recover the mail
                Mail newMail = request.getBody();

                // Update the mail list of every recipient in the list with the new mail
                for(String address : newMail.getRecipients()){
                    lastAddress = address;
                    mailboxes.updateMailboxMailList(address, newMail);
                }

                // Send an affirmative response to client
                sendOK(toClient,request, log);
            } catch (NoSuchElementException noSuchElementException){
                // If the given mail addresses are not present, associated with the first wrong one
                addressNotFoundError(toClient, lastAddress, request, log);
            } catch (Exception e){
                internalError(toClient, request, log);  // If something gone wrong
            }

        }
    }

    // Delete a mail based in his ID
    private class DeleteMail implements Runnable {
        private final ObjectOutputStream toClient;                      // The stream to the client
        private final Request request;                                  // The came request
        private final Log log;                                          // The log associated

        private DeleteMail(ObjectOutputStream toClient, Request request, Log log) {
            this.toClient = toClient;
            this.request = request;
            this.log = log;
        }

        @Override
        public void run() {
            try {

                // Delete the mail from the mail list
                mailboxes.deleteMailboxMail(request.getAddress(), request.getBody().getID());

                // Send an affirmative response to client
                sendOK(toClient, request, log);
            } catch (NoSuchElementException noSuchElementException) {
                // If the given mail address is not present
                addressNotFoundError(toClient, request.getAddress(), request, log);
            } catch (Exception e) {
                internalError(toClient, request, log);  // If something gone wrong
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // Send an empty affirmative response to the requester
    private void sendOK(ObjectOutputStream toClient, Request request, Log log){
        try{
            // Create a new Response with the OK code and send it
            Response response = new Response(Response.OK);
            toClient.writeObject(response);

            // Update the log associated properly
            Platform.runLater(() -> {
                log.setStatus(Mailboxes.TICK);
                log.setStatusText("Done");
            });

        } catch (IOException e){
            connectionError(log);
        }
    }

    // Send an affirmative response to the requester associated with the mail list
    private void sendOK(ObjectOutputStream toClient, Request request, List<Mail> mailList, Log log){
        try{
            // Create a new Response with the OK code, associate the mail list and send it
            Response response = new Response(Response.OK, mailList);
            toClient.writeObject(response);

            // Update the log associated properly
            Platform.runLater(() -> {
                log.setStatus(Mailboxes.TICK);
                log.setStatusText("Done");
            });

        } catch (IOException e){
            connectionError(log);
        }
    }

    // Send an internal error response to the requester
    private void internalError(ObjectOutputStream toClient, Request request, Log log) {
        try{
            // Create a new Response with the INTERNAL_ERROR code and send it
            Response response = new Response(Response.INTERNAL_ERROR);
            toClient.writeObject(response);

            // Update the log associated properly
            Platform.runLater(() -> {
                log.setStatus(Mailboxes.CROSS);
                log.setStatusText("Internal Error");
            });

        } catch (IOException e){
            connectionError(log);
        }

    }

    // Send an address not found error response to the requester associated with the mail address not present
    private void addressNotFoundError(ObjectOutputStream toClient, String address, Request request, Log log) {
        try {
            // Create a new Response with the ADDRESS_NOT_FOUND code, associate the wrong mail address and send it
            Response response = new Response(Response.ADDRESS_NOT_FOUND, address);
            toClient.writeObject(response);

            // Update the log associated properly
            Platform.runLater(() -> {
                log.setStatus(Mailboxes.CROSS);
                log.setStatusText("Address not found: " + address);
            });

        } catch (IOException e){
            connectionError(log);
        }

    }

    // Send an bad request error response to the requester
    private void badRequestError(ObjectOutputStream toClient){
        try {
            Response response = new Response(Response.BAD_REQUEST);
            toClient.writeObject(response);

            // TODO: log a bad request
            //Platform.runLater(() -> mailboxes.logsProperty().add(new Log(response, request)));
            //mailboxes.updateStatusLog(Mailboxes.FAILED, "Bad request");
        } catch (IOException e){
            // TODO: client disconnected / problems related with connection
            e.printStackTrace();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // Log a connection error when trying to send a response to the requester
    private void connectionError(Log log){
        Platform.runLater(() -> {
            log.setStatus(Mailboxes.CROSS);
            log.setStatusText("Disconnected / Connection errors ");
        });
    }
}
