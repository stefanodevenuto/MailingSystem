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
    private static final int MAX_CLIENTS = 2;
    //private GlobalMailbox globalMailbox;

    @FXML
    private ListView<Mail> logListView;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        ExecutorService executors = Executors.newFixedThreadPool(MAX_CLIENTS);

        try {
            ServerSocket acceptor = new ServerSocket(4444);
            while(true) {
                Socket client = acceptor.accept();
                System.out.println("Accettato");

                ObjectInputStream fromClient = new ObjectInputStream(client.getInputStream());
                ObjectOutputStream toClient = new ObjectOutputStream(client.getOutputStream());

                Object request = fromClient.readObject();

                if(!(request != null && request instanceof Request)) {
                    System.out.println("Server: BAD REQUEST"); // TODO: creare classi custom per le eccezioni
                }

                switch (((Request) request).getType()) {
                    case Request.GET_MAILLIST: {
                        Mailbox m = new Mailbox("first@gmail.com");
                        Runnable sendMailList = new SendMailList(toClient, m);
                        executors.execute(sendMailList);
                        break;
                    }

                }


            }
        } catch (/*IO*/Exception e) {
            e.printStackTrace();
        }
    }

    public class SendMailList implements Runnable {
        private ObjectOutputStream toClient;
        //private String clientAddress;
        //private GlobalMailbox globalMailbox;
        private Mailbox mailbox;

        public SendMailList(ObjectOutputStream client, Mailbox m) {
            toClient = client;
            mailbox = m;
        }

        @Override
        public void run() {
            try {
                toClient.writeObject(mailbox.getMailList());
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }


}
