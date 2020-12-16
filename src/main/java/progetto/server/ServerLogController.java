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
    private Mailboxes mailboxes;
    private ExecutorService executors;

    @FXML
    private TableView<Log> logsTable;

    @FXML
    private TableColumn<Log, String> dateColumn;

    @FXML
    private TableColumn<Log, String> requesterColumn;

    @FXML
    private TableColumn<Log, Request> requestColumn;

    @FXML
    private TableColumn<Log, Image> statusImageColumn;

    @FXML
    private TableColumn<Log, String> statusTextColumn;

    public StartListener initController(Mailboxes mailboxes, ExecutorService executors) {

        // ensure model is only set once
        if (this.mailboxes != null) {
            throw new IllegalStateException("Model can only be initialized once");
        }

        this.mailboxes = mailboxes;
        this.executors = executors;

        logsTable.setItems(mailboxes.logsProperty());

        dateColumn.setCellValueFactory(date -> date.getValue().dateTimeProperty());
        requesterColumn.setCellValueFactory(requester -> requester.getValue().requesterProperty());
        requestColumn.setCellValueFactory(request -> request.getValue().requestProperty());

        statusImageColumn.setCellFactory(logImageViewTableColumn -> {
            final ImageView imageView = new ImageView();
            imageView.setFitHeight(24);
            imageView.setFitWidth(24);

            //Set up the Table
            TableCell<Log, Image> cell = new TableCell<>() {
                public void updateItem(Image item, boolean empty) {
                    imageView.setImage(item);
                }
            };
            // Attach the imageview to the cell
            cell.setGraphic(imageView);
            return cell;
        });

        statusImageColumn.setCellValueFactory(status -> status.getValue().statusProperty()/*new PropertyValueFactory<>("status")*/);
        statusTextColumn.setCellValueFactory(statusText -> statusText.getValue().statusTextProperty());

        StartListener startListener = new StartListener();
        this.executors.execute(startListener);

        return startListener;
    }


    public class StartListener implements Runnable{

        private ServerSocket acceptor = null;

        @Override
        public void run() {
            try {
                acceptor = new ServerSocket(4444);
                while(true) {
                    Socket client = acceptor.accept();

                    Runnable requestsHandler = new RequestsHandler(client, executors);
                    executors.execute(requestsHandler);
                }
            } catch (IOException e) {
                // Socket is closed
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        public void closeSocket() throws IOException {
            if(acceptor != null)
                acceptor.close();
        }
    }

    private class RequestsHandler implements Runnable {
        private final Socket client;
        private final ExecutorService executorService;

        private RequestsHandler(Socket client, ExecutorService executorService) {
            this.client = client;
            this.executorService = executorService;
        }

        @Override
        public void run() {
            try {
                ObjectInputStream fromClient = new ObjectInputStream(client.getInputStream());
                ObjectOutputStream toClient = new ObjectOutputStream(client.getOutputStream());

                Object request = fromClient.readObject();


                if(!(request instanceof Request)) {
                    System.out.println("Server: BAD REQUEST");
                    badRequestError(toClient);
                    return;
                }

                Request r = (Request) request;
                Log log = new Log(r.getAddress(), r, Mailboxes.LOAD);

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

                mailboxes.logsProperty().add(log);

            } catch (IOException | ClassNotFoundException e) {
                //connectionError(r);
                e.printStackTrace();
            }
        }
    }

    private class SendMailList implements Runnable {
        private final ObjectOutputStream toClient;
        private final Request request;
        private final Log log;

        private SendMailList(ObjectOutputStream toClient, Request request, Log log) {
            this.toClient = toClient;
            this.request = request;
            this.log = log;
        }

        @Override
        public void run() {
            try {
                List<Mail> mailList = mailboxes.getMailboxMailList(request.getAddress(), request.getCounter());
                sendOK(toClient, request, mailList, log);
            } catch(NoSuchElementException noSuchElementException) {
                addressNotFoundError(toClient, request.getAddress(), request, log);
            } catch (Exception e){
                internalError(toClient, request, log);
            }
        }
    }

    private class WriteMail implements Runnable{
        private final ObjectOutputStream toClient;
        private final Request request;
        private Log log;

        private WriteMail(ObjectOutputStream toClient, Request request, Log log) {
            this.toClient = toClient;
            this.request = request;
            this.log = log;
        }

        @Override
        public void run() {
            String lastAddress = null;
            try {
                Mail newMail = request.getBody();
                for(String address : newMail.getRecipients()){
                    lastAddress = address;
                    mailboxes.updateMailboxMailList(address, newMail);
                }

                sendOK(toClient,request, log);
            } catch (NoSuchElementException noSuchElementException){
                addressNotFoundError(toClient, lastAddress, request, log);
            } catch (Exception e){
                internalError(toClient, request, log);
            }

        }
    }

    private class DeleteMail implements Runnable {
        private final ObjectOutputStream toClient;
        private final Request request;
        private Log log;

        private DeleteMail(ObjectOutputStream toClient, Request request, Log log) {
            this.toClient = toClient;
            this.request = request;
            this.log = log;
        }

        @Override
        public void run() {
            try {
                mailboxes.deleteMailboxMail(request.getAddress(), request.getBody().getID());
                sendOK(toClient, request, log);
            } catch (NoSuchElementException noSuchElementException) {
                addressNotFoundError(toClient, request.getAddress(), request, log);
            } catch (Exception e) {
                internalError(toClient, request, log);
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void sendOK(ObjectOutputStream toClient, Request request, Log log){
        try{
            Response response = new Response(Response.OK);
            toClient.writeObject(response);

            Platform.runLater(() -> {
                log.setStatus(Mailboxes.TICK);
                log.setStatusText("Done");
            });

        } catch (IOException e){
            connectionError(log);
        }
    }

    private void sendOK(ObjectOutputStream toClient, Request request, List<Mail> mailList, Log log){
        try{
            Response response = new Response(Response.OK, mailList);
            toClient.writeObject(response);

            Platform.runLater(() -> {
                log.setStatus(Mailboxes.TICK);
                log.setStatusText("Done");
            });

        } catch (IOException e){
            connectionError(log);
        }
    }

    private void internalError(ObjectOutputStream toClient, Request request, Log log) {
        try{
            Response response = new Response(Response.INTERNAL_ERROR);
            toClient.writeObject(response);

            Platform.runLater(() -> {
                log.setStatus(Mailboxes.CROSS);
                log.setStatusText("Internal Error");
            });

        } catch (IOException e){
            connectionError(log);
        }

    }

    private void addressNotFoundError(ObjectOutputStream toClient, String address, Request request, Log log) {
        try {
            Response response = new Response(Response.ADDRESS_NOT_FOUND, address);
            toClient.writeObject(response);

            Platform.runLater(() -> {
                log.setStatus(Mailboxes.CROSS);
                log.setStatusText("Address not found: " + address);
            });

        } catch (IOException e){
            connectionError(log);
        }

    }

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

    private void connectionError(Log log){
        Platform.runLater(() -> {
            log.setStatus(Mailboxes.CROSS);
            log.setStatusText("Disconnected / Connection errors ");
        });
    }
}
