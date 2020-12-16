package progetto.server;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Callback;
import progetto.common.Mail;
import javafx.fxml.FXML;
import progetto.common.Request;
import progetto.common.Response;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;

public class ServerLogController {
    private Mailboxes mailboxes;
    private ExecutorService executors;

    private static Image load;
    private static Image tick;
    private static Image cross;

    @FXML
    private ListView<Log> logListView;

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

        try {
            load = new Image(getClass().getResource("/progetto.server/load.jpg").toExternalForm());
            cross = new Image(getClass().getResource("/progetto.server/cross.jpg").toExternalForm());
            tick = new Image(getClass().getResource("/progetto.server/tick.jpg").toExternalForm());
        } catch (Exception e){
            e.printStackTrace();
        }

        // Set the entire MailList (ObservableList<Request>) to the ListView
        //logListView.setItems(mailboxes.logsProperty());

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

        // Show the title of the Mail for each one in the ObservableList
        /*logListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            public void updateItem(Log l, boolean empty) {
                super.updateItem(l, empty);
                if (empty) {
                    setText(null);
                } else {
                    setText(l.logText());
                }
            }
        });*/

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
                while(true) { // TODO: diventa while(!interrupted)
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

                Log log = new Log(r.getAddress(), r, load);

                switch (r.getType()) {
                    /*case Request.UPDATE_MAILLIST:
                    case Request.GET_FULL_MAILLIST: {
                        System.out.println("Recover mail...");
                        Runnable sendMailList = new SendMailList(toClient, r, log);
                        executorService.execute(sendMailList);
                        break;
                    }*/

                    case Request.SEND:{
                        System.out.println("Reply mail...: " + r.getBody().getRecipients());
                        Runnable writeMail = new WriteMail(toClient, r, log);
                        executorService.execute(writeMail);
                        break;
                    }

                    case Request.DELETE:{
                        System.out.println("Delete mail...: " + r.getBody());
                        Runnable deleteMail = new DeleteMail(toClient, r, log);
                        executorService.execute(deleteMail);
                        break;
                    }

                    case Request.MAILLIST:{
                        System.out.println("Miao mail...");
                        Runnable deleteMail = new SendMailList(toClient, r, log);
                        executorService.execute(deleteMail);
                        break;
                    }

                }

                Platform.runLater(() -> mailboxes.logsProperty().add(log));

            } catch (IOException | ClassNotFoundException e) {
                // TODO: client disconnected / problems related with connection
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

                for(Mail m : mailList){
                    System.out.println("Server controller ID: " + m.getID());
                }

                sendOK(toClient, request, mailList, log);
            } catch(NoSuchElementException noSuchElementException) {
                addressNotFoundError(toClient, request.getAddress(), request, log);
            } catch (Exception e){
                e.printStackTrace();
                internalError(toClient, request, log);
            }
        }
    }

    /*private class SendMailList implements Runnable {
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
                boolean type = false;
                if(request.getType() == Request.UPDATE_MAILLIST)
                    type = true;

                List<Mail> mailList = mailboxes.getMailboxMailList(request.getAddress(), type);

                for(Mail m : mailList){
                    System.out.println("Server controller ID: " + m.getID());
                }

                sendOK(toClient, request, mailList, log);
            } catch(NoSuchElementException noSuchElementException) {
                addressNotFoundError(toClient, request.getAddress(), request, log);
            } catch (Exception e){
                e.printStackTrace();
                internalError(toClient, request, log);
            }
        }
    }*/

    private class WriteMail implements Runnable{
        private final ObjectOutputStream toClient;
        private final Request request;
        private final Log log;

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
                    System.out.println("Invio a: " + address);
                    lastAddress = address;
                    mailboxes.updateMailboxMailList(address, newMail);
                }

                sendOK(toClient,request, log);
            } catch (NoSuchElementException noSuchElementException){
                addressNotFoundError(toClient, lastAddress, request, log);
            } catch (Exception e){
                e.printStackTrace();
                internalError(toClient, request, log);
            }

        }
    }

    private class DeleteMail implements Runnable {
        private final ObjectOutputStream toClient;
        private final Request request;
        private final Log log;

        private DeleteMail(ObjectOutputStream toClient, Request request, Log log) {
            this.toClient = toClient;
            this.request = request;
            this.log = log;
        }

        @Override
        public void run() {
            try {
                System.out.println("Deleting: " + request.getBody().getID());
                mailboxes.deleteMailboxMail(request.getAddress(), request.getBody().getID());

                sendOK(toClient, request, log);
            } catch (NoSuchElementException noSuchElementException) {
                addressNotFoundError(toClient, request.getAddress(), request, log);
            } catch (Exception e) {
                e.printStackTrace();
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
                log.setStatus(tick);
                log.setStatusText("Done");
            });
        } catch (IOException e){
            // TODO: client disconnected / problems related with connection
            e.printStackTrace();
        }
    }

    private void sendOK(ObjectOutputStream toClient, Request request, List<Mail> mailList, Log log){
        try{
            Response response = new Response(Response.OK, mailList);
            toClient.writeObject(response);

            Platform.runLater(() -> {
                log.setStatus(tick);
                log.setStatusText("Done");
            });
        } catch (IOException e){
            // TODO: client disconnected / problems related with connection
            e.printStackTrace();
        }
    }

    private void internalError(ObjectOutputStream toClient, Request request, Log log) {
        try{
            Response response = new Response(Response.INTERNAL_ERROR);
            toClient.writeObject(response);

            Platform.runLater(() -> {
                log.setStatus(cross);
                log.setStatusText("Internal Error");
            });
        } catch (IOException e){
            // TODO: client disconnected / problems related with connection
            e.printStackTrace();
        }

    }

    private void addressNotFoundError(ObjectOutputStream toClient, String address, Request request, Log log) {
        try{
            Response response = new Response(Response.ADDRESS_NOT_FOUND, address);
            toClient.writeObject(response);

            Platform.runLater(() -> {
                log.setStatus(cross);
                log.setStatusText("Address not found: " + address);
            });
        } catch (IOException e){
            // TODO: client disconnected / problems related with connection
            e.printStackTrace();
        }

    }

    private void badRequestError(ObjectOutputStream toClient){
        try{
            Response response = new Response(Response.BAD_REQUEST);
            toClient.writeObject(response);

            // TODO: log a bad request
            //Platform.runLater(() -> mailboxes.logsProperty().add(new Log(response, request)));
        } catch (IOException e){
            // TODO: client disconnected / problems related with connection
            e.printStackTrace();
        }
    }

    /*private class ImageCell<T> extends TableCell<T, ImageView> {
        private final ImageView image;

        public ImageCell() {
            // add ImageView as graphic to display it in addition
            // to the text in the cell
            image = new ImageView();
            image.setFitWidth(24);
            image.setFitHeight(24);
            image.setPreserveRatio(true);

            setGraphic(image);
            setMinHeight(24);
        }


        @Override
        protected void updateItem(ImageView log, boolean empty) {
            super.updateItem(log, empty);

            if (empty || log == null) {
                // set back to look of empty cell
                image.setImage(null);
            } else {
                // set image and text for non-empty cell
                image.setImage(log.getImage());
            }
        }
    }*/

}
