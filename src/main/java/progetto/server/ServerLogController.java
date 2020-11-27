package progetto.server;

import javafx.fxml.Initializable;
import progetto.common.Mail;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import progetto.common.Request;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerLogController implements Initializable {
    private static final int MAX_CLIENTS = 5;
    private Mailboxes mailboxes;

    @FXML
    private ListView<Mail> logListView;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        ExecutorService executors = Executors.newFixedThreadPool(MAX_CLIENTS);
        mailboxes = new Mailboxes();

        try {
            ServerSocket acceptor = new ServerSocket(4444);
            while(true) {
                Socket client = acceptor.accept();
                System.out.println("Accettato");

                ObjectInputStream fromClient = new ObjectInputStream(client.getInputStream());
                ObjectOutputStream toClient = new ObjectOutputStream(client.getOutputStream());

                Runnable requestsHandler = new RequestsHandler(client, fromClient, toClient, executors);
                executors.execute(requestsHandler);
            }
        } catch (/*IO*/Exception e) {
            e.printStackTrace();
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

                switch (((Request) request).getType()) {
                    case Request.GET_MAILLIST: {
                        Runnable sendMailList = new SendMailList(toClient, "first@gmail.com");
                        executorService.execute(sendMailList);
                        break;
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class SendMailList implements Runnable {
        private String address;
        private ObjectOutputStream toClient;

        private SendMailList(ObjectOutputStream toClient, String address) {
            this.toClient = toClient;
            this.address = address;
        }

        @Override
        public void run() {
            try {
                toClient.writeObject(mailboxes.getMailboxMailist(address));
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }


}
