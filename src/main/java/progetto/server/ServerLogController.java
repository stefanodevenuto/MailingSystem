package progetto.server;

import javafx.application.Platform;
import javafx.fxml.Initializable;
import javafx.scene.control.ListCell;
import progetto.common.Mail;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import progetto.common.Request;
import progetto.common.Response;

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

public class ServerLogController implements Initializable {
    private static final int MAX_CLIENTS = 20;
    private Mailboxes mailboxes;

    @FXML
    private ListView<Request> logListView;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        mailboxes = new Mailboxes();

        // Set the entire MailList (ObservableList<Mail>) to the ListView
        logListView.setItems(mailboxes.logsProperty());

        // Show the title of the Mail for each one in the ObservableList
        logListView.setCellFactory(lv -> new ListCell<Request>() {
            @Override
            public void updateItem(Request r, boolean empty) {
                super.updateItem(r, empty);
                if (empty) {
                    setText(null);
                } else {
                    switch (r.getType()) {
                        case Request.GET_FULL_MAILLIST: {
                            setText("Request of full mailList started by " + r.getAddress());
                            break;
                        }

                        case Request.UPDATE_MAILLIST:{
                            setText("Request of incremental mailList by " + r.getAddress());
                            break;
                        }

                        case Request.SEND:{
                            Mail m = r.getBody();
                            StringBuilder s = new StringBuilder("Send mail request by " + r.getAddress() + " to ");
                            for(String recipient : m.getRecipients()){
                                s.append(recipient).append(", ");
                            }
                            s.deleteCharAt(s.length()-1);
                            setText(s.toString());
                            break;
                        }

                        default:{
                            StringBuilder s = new StringBuilder("Bad request from ");
                            if(r.getAddress() != null){
                                s.append(r.getAddress());
                            } else {
                                s.append("Unknown");
                            }
                            break;
                        }

                    }
                }
            }
        });

        Runnable startListener = new StartListener();
        Thread t = new Thread(startListener);
        t.start();
    }

    public class StartListener implements Runnable{

        @Override
        public void run() {
            System.out.println("Ascolto");
            ExecutorService executors = Executors.newFixedThreadPool(MAX_CLIENTS);
            try {
                ServerSocket acceptor = new ServerSocket(4444);
                while(true) {
                    Socket client = acceptor.accept();
                    //System.out.println("Accettato");

                    ObjectInputStream fromClient = new ObjectInputStream(client.getInputStream());
                    ObjectOutputStream toClient = new ObjectOutputStream(client.getOutputStream());

                    Runnable requestsHandler = new RequestsHandler(client, fromClient, toClient, executors);
                    executors.execute(requestsHandler);
                }
            } catch (/*IO*/Exception e) {
                e.printStackTrace();
            }
        }
    }

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
                    System.out.println("Server: BAD REQUEST"); // TODO: creare classi custom per le eccezioni
                }

                Request r = (Request) request;

                switch (r.getType()) {
                    case Request.UPDATE_MAILLIST:
                    case Request.GET_FULL_MAILLIST: {
                        //System.out.println("Recupero mail...");
                        Runnable sendMailList = new SendMailList(toClient, r.getAddress(), r.getType());
                        executorService.execute(sendMailList);
                        break;
                    }

                    case Request.SEND:{
                        System.out.println("Reply mail...: " + r.getBody().getRecipients());
                        Runnable writeMail = new WriteMail(toClient, r.getBody());
                        executorService.execute(writeMail);
                        break;
                    }

                    case Request.DELETE:{
                        System.out.println("Delete mail...: " + r.getBody());
                        Runnable deleteMail = new DeleteMail(toClient, r.getAddress(), r.getBody());
                        executorService.execute(deleteMail);
                        break;
                    }

                }

                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        logListView.getItems().add(r);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class SendMailList implements Runnable {
        private String address;
        private ObjectOutputStream toClient;
        private int requestType;

        private SendMailList(ObjectOutputStream toClient, String address, int requestType) {
            this.toClient = toClient;
            this.address = address;
            this.requestType = requestType;
        }

        @Override
        public void run() {
            try {
                boolean type = false;
                if(requestType == Request.UPDATE_MAILLIST)
                    type = true;
                toClient.writeObject(new Response(Response.OK, mailboxes.getMailboxMailist(address, type)));
            } catch(NoSuchElementException noSuchElementException) {
                try {
                    toClient.writeObject(new Response(Response.ADDRESS_NOT_FOUND));
                } catch (Exception e){
                    e.printStackTrace();
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private class WriteMail implements Runnable{
        private Mail newMail;
        private ObjectOutputStream toClient;

        private WriteMail(ObjectOutputStream toClient, Mail newMail) {
            this.toClient = toClient;
            this.newMail = newMail;
        }

        @Override
        public void run() {
            try {
                for(String address : newMail.getRecipients()){
                    System.out.println("Invio a: " + address);
                    mailboxes.updateMailboxMailist(address, newMail);
                }
                toClient.writeObject(new Response(Response.OK));
            } catch (NoSuchElementException noSuchElementException){
                try {
                    toClient.writeObject(new Response(Response.ADDRESS_NOT_FOUND));
                } catch (Exception e){
                    e.printStackTrace();
                }
            } catch (Exception e){
                e.printStackTrace();
            }

        }
    }

    private class DeleteMail implements Runnable {
        private Mail mail;
        private ObjectOutputStream toClient;
        private String address;

        private DeleteMail(ObjectOutputStream toClient, String address, Mail mail) {
            this.toClient = toClient;
            this.address = address;
            this.mail = mail;
        }

        @Override
        public void run() {
            try {
                System.out.println("Deleting");
                mailboxes.deleteMailboxMail(address, mail.getID());

                toClient.writeObject(new Response(Response.OK));
            } catch (NoSuchElementException noSuchElementException){
                try {
                    toClient.writeObject(new Response(Response.ADDRESS_NOT_FOUND));
                } catch (Exception e){
                    e.printStackTrace();
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

}
