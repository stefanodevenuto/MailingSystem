package progetto.client;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import progetto.common.Mail;
import progetto.common.Request;
import progetto.common.Response;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class NewMailController {
    private Mailbox mailbox;
    private HashMap<String, Pane> screenMap;
    private BorderPane root;

    @FXML
    private TextField currentTitle;

    @FXML
    private TextField currentRecipientsTextField;

    @FXML
    private ListView<String> currentRecipientsListView;

    @FXML
    private TextArea currentText;

    @FXML
    public void handleSendButton(ActionEvent actionEvent) {
        mailbox.getCurrentMail().setSender(mailbox.getAddress());
        try {
            Socket server = new Socket("localhost", 4444);

            try {
                ObjectOutputStream toServer = new ObjectOutputStream(server.getOutputStream());
                ObjectInputStream fromServer = new ObjectInputStream(server.getInputStream());
                try{
                    toServer.writeObject(new Request(Request.SEND, mailbox.getAddress(), mailbox.getCurrentMail()));

                    Object o = fromServer.readObject();

                    if(o != null && o instanceof Response){
                        System.out.println(((Response)o).getCode());
                        // TODO: refresh della lista di mail


                    }
                } finally {
                    toServer.close();
                    fromServer.close();
                }

            } finally {
                System.out.println("Chiuso");
                server.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void initController(Mailbox mailbox, HashMap<String, Pane> screenMap, BorderPane root) {
        // ensure model is only set once:
        if (this.mailbox != null) {
            throw new IllegalStateException("Model can only be initialized once");
        }

        this.mailbox = mailbox;
        this.screenMap = screenMap;
        this.root = root;

        //mailbox.setCurrentMail(new Mail());

        ChangeListener<Boolean> recipientsListener = (observable, oldProperty, newProperty) -> {
            if(!newProperty){
                String givenRecipients = currentRecipientsTextField.getText();
                String[] recipients = givenRecipients.split(";");

                mailbox.getCurrentMail().setRecipients(new ArrayList<>(Arrays.asList(recipients)));

                System.out.println(Arrays.toString(recipients));

            }else{
                System.out.println("2");
            }
        };

        this.mailbox.currentMailProperty().addListener(new ChangeListener<Mail>() {
            @Override
            public void changed(ObservableValue observable, Mail oldMail, Mail newMail) {

                if(newMail != null && newMail.getRecipients().isEmpty()){
                    System.out.println("Arrivo da una Forward/New");
                    currentRecipientsTextField.setVisible(true);
                    currentRecipientsListView.setVisible(false);
                } else {
                    //System.out.println("Arrivo da una Reply/Reply All: " + newMail.getRecipients());
                    currentRecipientsTextField.setVisible(false);
                    currentRecipientsListView.setVisible(true);
                }

                if (oldMail != null) {
                    currentTitle.textProperty().unbindBidirectional(oldMail.titleProperty());
                    currentRecipientsTextField.focusedProperty().removeListener(recipientsListener);
                    currentRecipientsListView.setItems(null);
                    currentText.textProperty().unbindBidirectional(oldMail.textProperty());
                }
                if (newMail == null) {
                    currentTitle.setText("");
                    currentRecipientsTextField.setText("");
                    currentRecipientsListView.setItems(null);
                    currentText.setText("");

                } else {
                    currentTitle.textProperty().bindBidirectional(newMail.titleProperty());
                    currentRecipientsTextField.focusedProperty().addListener(recipientsListener);
                    currentRecipientsListView.setItems(newMail.recipientsProperty());
                    currentText.textProperty().bindBidirectional(newMail.textProperty());
                }
            }
        });
    }


}
