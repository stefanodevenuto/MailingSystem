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
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;

public class ServerLogController {
    private Mailboxes mailboxes;
    private ExecutorService executors;

    private static FileInputStream load;
    private static FileInputStream tick;
    private static FileInputStream cross;

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
    private TableColumn<Log, ImageView> statusColumn;

    public void initController(Mailboxes mailboxes, ExecutorService executors) {

        // ensure model is only set once
        if (this.mailboxes != null) {
            throw new IllegalStateException("Model can only be initialized once");
        }

        this.mailboxes = mailboxes;
        this.executors = executors;



        try {
            load = new FileInputStream("C:\\Users\\stefa\\Desktop\\load.jpg");
            cross = new FileInputStream("C:\\Users\\stefa\\Desktop\\cross.jpg");
            tick = new FileInputStream("C:\\Users\\stefa\\Desktop\\tick.jpg");
        } catch (Exception e){
            e.printStackTrace();
        }

        // Set the entire MailList (ObservableList<Request>) to the ListView
        //logListView.setItems(mailboxes.logsProperty());

        logsTable.setItems(mailboxes.logsProperty());

        dateColumn.setCellValueFactory(date -> date.getValue().dateTimeProperty());
        requesterColumn.setCellValueFactory(requester -> requester.getValue().requesterProperty());
        requestColumn.setCellValueFactory(request -> request.getValue().requestProperty());
        statusColumn.setCellValueFactory(date -> date.getValue().statusProperty());

        statusColumn.setCellFactory(new Callback<TableColumn<Log, ImageView>, TableCell<Log, ImageView>>() {
            @Override
            public TableCell<Log, ImageView> call(TableColumn<Log, ImageView> logImageViewTableColumn) {
                return new TableCell<>();
            }
        });

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

        Runnable startListener = new StartListener();
        this.executors.execute(startListener);
    }


    public class StartListener implements Runnable{

        @Override
        public void run() {
            try {
                ServerSocket acceptor = new ServerSocket(4444);
                while(true) {
                    Socket client = acceptor.accept();
                    //System.out.println("Accettato");

                    Runnable requestsHandler = new RequestsHandler(client, executors);
                    executors.execute(requestsHandler);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class RequestsHandler implements Runnable {
        private final Socket client;
        private final ExecutorService executorService;
        private int requestID = 0;

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

                Log log = new Log(r.getAddress(), r, new ImageView(new Image(load)));

                switch (r.getType()) {
                    case Request.UPDATE_MAILLIST:
                    case Request.GET_FULL_MAILLIST: {
                        System.out.println("Recover mail...");
                        Runnable sendMailList = new SendMailList(toClient, r, log);
                        executorService.execute(sendMailList);
                        break;
                    }

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
    }

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

            Platform.runLater(() -> log.setStatus(new ImageView(new Image(tick))));
        } catch (IOException e){
            // TODO: client disconnected / problems related with connection
            e.printStackTrace();
        }
    }

    private void sendOK(ObjectOutputStream toClient, Request request, List<Mail> mailList, Log log){
        try{
            Response response = new Response(Response.OK, mailList);
            toClient.writeObject(response);

            Platform.runLater(() -> log.setStatus(new ImageView(new Image(tick))));
        } catch (IOException e){
            // TODO: client disconnected / problems related with connection
            e.printStackTrace();
        }
    }

    private void internalError(ObjectOutputStream toClient, Request request, Log log) {
        try{
            Response response = new Response(Response.INTERNAL_ERROR);
            toClient.writeObject(response);

            Platform.runLater(() -> log.setStatus(new ImageView(new Image(cross))));
        } catch (IOException e){
            // TODO: client disconnected / problems related with connection
            e.printStackTrace();
        }

    }

    private void addressNotFoundError(ObjectOutputStream toClient, String address, Request request, Log log) {
        try{
            Response response = new Response(Response.ADDRESS_NOT_FOUND, address);
            toClient.writeObject(response);

            Platform.runLater(() -> log.setStatus(new ImageView(new Image(cross))));
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
        protected void updateItem(Log log, boolean empty) {
            super.updateItem(log, empty);

            if (empty || log == null) {
                // set back to look of empty cell
                image.setImage(null);
            } else {
                // set image and text for non-empty cell
                image.setImage(log.requestProperty());
            }
        }
    }*/

}
