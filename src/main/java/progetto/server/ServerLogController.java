package progetto.server;

import javafx.application.Platform;
import javafx.fxml.Initializable;
import javafx.scene.control.ListCell;
import progetto.client.model.Mailbox;
import progetto.common.Mail;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import progetto.common.Request;
import progetto.common.Response;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerLogController {
    private Mailboxes mailboxes;
    private ExecutorService executors;

    @FXML
    private ListView<Log> logListView;

    public void setExecutors(ExecutorService executors){
        this.executors = executors;
    }

    public void initController(Mailboxes mailboxes, ExecutorService executors){

        // ensure model is only set once
        if (this.mailboxes != null) {
            throw new IllegalStateException("Model can only be initialized once");
        }

        this.mailboxes = mailboxes;
        this.executors = executors;


        // Set the entire MailList (ObservableList<Request>) to the ListView
        logListView.setItems(mailboxes.logsProperty());

        // Show the title of the Mail for each one in the ObservableList
        logListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            public void updateItem(Log l, boolean empty) {
                super.updateItem(l, empty);
                if (empty) {
                    setText(null);
                } else {
                    setText(l.logText());
                }
            }
        });

        Runnable startListener = new StartListener();
        executors.execute(startListener);
    }


    public class StartListener implements Runnable{

        @Override
        public void run() {
            try {
                ServerSocket acceptor = new ServerSocket(4444);
                while(true) {
                    Socket client = acceptor.accept();
                    //System.out.println("Accettato");

                    ObjectInputStream fromClient = new ObjectInputStream(client.getInputStream());
                    ObjectOutputStream toClient = new ObjectOutputStream(client.getOutputStream());

                    Runnable requestsHandler = new RequestsHandler(client, fromClient, toClient, executors);
                    //Runnable requestsHandler = new RequestsHandler(client);
                    executors.execute(requestsHandler);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /*private class RequestsHandler implements Runnable {
        private Socket client;

        private RequestsHandler(Socket client){
            this.client = client;
        }

        @Override
        public void run() {

        }
    }*/


    private class RequestsHandler implements Runnable {
        private Socket client;
        private ObjectInputStream fromClient;
        private ObjectOutputStream toClient;
        private ExecutorService executorService;

        private RequestsHandler(Socket client, ObjectInputStream fromClient, ObjectOutputStream toClient, ExecutorService executorService) {
            this.client = client;
            this.fromClient = fromClient;
            this.toClient = toClient;
            this.executorService = executorService;
        }

        @Override
        public void run() {
            try {
                Object request = fromClient.readObject();

                if(!(request != null && request instanceof Request)) {
                    System.out.println("Server: BAD REQUEST");
                }

                Request r = (Request) request;

                switch (r.getType()) {
                    case Request.UPDATE_MAILLIST:
                    case Request.GET_FULL_MAILLIST: {
                        //System.out.println("Recupero mail...");
                        Runnable sendMailList = new SendMailList(toClient, r);
                        executorService.execute(sendMailList);
                        break;
                    }

                    case Request.SEND:{
                        System.out.println("Reply mail...: " + r.getBody().getRecipients());
                        Runnable writeMail = new WriteMail(toClient, r);
                        executorService.execute(writeMail);
                        break;
                    }

                    case Request.DELETE:{
                        System.out.println("Delete mail...: " + r.getBody());
                        Runnable deleteMail = new DeleteMail(toClient, r);
                        executorService.execute(deleteMail);
                        break;
                    }

                }

                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        mailboxes.logsProperty().add(new Log(r));
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class SendMailList implements Runnable {
        private ObjectOutputStream toClient;
        private Request request;

        private SendMailList(ObjectOutputStream toClient, Request request) {
            this.toClient = toClient;
            this.request = request;
        }

        @Override
        public void run() {
            try {
                boolean type = false;
                if(request.getType() == Request.UPDATE_MAILLIST)
                    type = true;

                List<Mail> mailList = mailboxes.getMailboxMailist(request.getAddress(), type);

                for(Mail m : mailList){
                    System.out.println("Server controller ID: " + m.getID());
                }
                Response response = new Response(Response.OK, mailList);
                toClient.writeObject(response);

                Platform.runLater(() -> mailboxes.logsProperty().add(new Log(response, request)));
            } catch(NoSuchElementException noSuchElementException) {
                try {
                    Response response = new Response(Response.ADDRESS_NOT_FOUND, request.getAddress());
                    toClient.writeObject(response);

                    Platform.runLater(() -> mailboxes.logsProperty().add(new Log(response, request)));
                } catch (Exception e){
                    e.printStackTrace(); // TODO: inviare internal error
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private class WriteMail implements Runnable{
        private ObjectOutputStream toClient;
        private Request request;

        private WriteMail(ObjectOutputStream toClient, Request request) {
            this.toClient = toClient;
            this.request = request;
        }

        @Override
        public void run() {
            String lastAddress = null;
            try {
                Mail newMail = request.getBody();
                for(String address : newMail.getRecipients()){
                    System.out.println("Invio a: " + address);
                    lastAddress = address;
                    mailboxes.updateMailboxMailist(address, newMail);
                }
                Response response = new Response(Response.OK);
                toClient.writeObject(response);

                Platform.runLater(() -> mailboxes.logsProperty().add(new Log(response, request)));
            } catch (NoSuchElementException noSuchElementException){
                try {
                    Response response = new Response(Response.ADDRESS_NOT_FOUND, lastAddress);
                    toClient.writeObject(response);

                    Platform.runLater(() -> mailboxes.logsProperty().add(new Log(response, request)));
                } catch (Exception e){
                    e.printStackTrace(); // Client disconnesso
                }
            } catch (Exception e){
                e.printStackTrace();
            }

        }
    }

    private class DeleteMail implements Runnable {
        private ObjectOutputStream toClient;
        private Request request;

        private DeleteMail(ObjectOutputStream toClient, Request request) {
            this.toClient = toClient;
            this.request = request;
        }

        @Override
        public void run() {
            try {
                System.out.println("Deleting: " + request.getBody().getID());
                mailboxes.deleteMailboxMail(request.getAddress(), request.getBody().getID());

                Response response = new Response(Response.OK);
                toClient.writeObject(response);

                Platform.runLater(() -> mailboxes.logsProperty().add(new Log(response, request)));
            } catch (NoSuchElementException noSuchElementException){
                try {
                    Response response = new Response(Response.ADDRESS_NOT_FOUND, request.getAddress());
                    toClient.writeObject(response);

                    Platform.runLater(() -> mailboxes.logsProperty().add(new Log(response, request)));
                } catch (Exception e){
                    e.printStackTrace();
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

}
