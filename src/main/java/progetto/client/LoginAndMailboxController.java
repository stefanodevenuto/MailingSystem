package progetto.client;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import progetto.common.Mail;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import progetto.common.Request;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class LoginAndMailboxController {

    private Mailbox mailbox;
    public static final Pattern EMAIL_ADDRESS_REGEX =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);

    @FXML
    private TextField insertedMail;

    @FXML
    private ListView<Mail> mailListView;

    public void initModel(Mailbox mailbox) {
        // ensure model is only set once
        if (this.mailbox != null) {
            throw new IllegalStateException("Model can only be initialized once");
        }

        this.mailbox = mailbox;
    }

    @FXML
    public void handleLoginButton(ActionEvent actionEvent) {
        String givenMailAddress = insertedMail.getText();

        if(!validate(givenMailAddress)) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Wrong email");
            alert.setContentText("Please insert a valid email");

            alert.showAndWait();
            return;
        }

        try {
            Socket server = new Socket("localhost", 4444);

            try {
                ObjectOutputStream toServer = new ObjectOutputStream(server.getOutputStream());
                ObjectInputStream fromServer = new ObjectInputStream(server.getInputStream());

                toServer.writeObject(new Request(Request.GET_MAILLIST, givenMailAddress));
                System.out.println("Request sent");
                Object o = fromServer.readObject();



                // Safe cast
                if(!(o instanceof List)) {
                    System.out.println("Client: MAILLIST FAILED");
                    return;
                }

                List<Mail> m = (List<Mail>) o;

                // Retrieve the original ObservableList type
                ObservableList<Mail> mailList = FXCollections.observableArrayList(m);

                mailbox.setCurrentMailList(mailList);

            } finally {
                System.out.println("Chiuso");
                server.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Set the entire MailList (ObservableList<Mail>) to the ListView
        mailListView.setItems(mailbox.currentMailListProperty());

        // Show the title of the Mail for each one in the ObservableList
        mailListView.setCellFactory(lv -> new ListCell<Mail>() {
            @Override
            public void updateItem(Mail mail, boolean empty) {
                super.updateItem(mail, empty);
                if (empty) {
                    setText(null);
                } else {
                    setText(mail.toString());
                }
            }
        });

        mailListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                Mail a = (Mail) newValue;
                mailbox.setCurrentMail(a);
            }
        });
    }

    public static boolean validate(String email) {
        Matcher matcher = EMAIL_ADDRESS_REGEX.matcher(email);
        return matcher.find();
    }
}
