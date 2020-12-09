package progetto.client;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.RowConstraints;
import progetto.common.Mail;
import progetto.common.Request;
import progetto.common.Response;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.NoSuchElementException;

public class NewMailController {
    private Mailbox mailbox;
    private Requester requester;

    private HashMap<String, Pane> screenMap;
    private BorderPane root;

    private static final int LIST_CELL_HEIGHT = 24;

    @FXML
    private TextField currentTitle;

    @FXML
    private TextField currentRecipientsTextField;

    @FXML
    private ListView<String> currentRecipientsListView;

    @FXML
    private RowConstraints recipientsRow;

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
                        //System.out.println(((Response)o).getCode());
                        // TODO: refresh della lista di mail
                        Response r = (Response) o;
                        if(r.getCode() == Response.OK){
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("Done");
                            alert.setHeaderText(null);
                            alert.setContentText("Mail sent successfully!");

                            alert.showAndWait();

                        }else if(r.getCode() == Response.ADDRESS_NOT_FOUND){
                            Alert alert = new Alert(Alert.AlertType.WARNING);
                            alert.setTitle("Wrong email");
                            alert.setHeaderText(null);
                            alert.setContentText("The inserted mail address "+ r.getError() + " doesn't exist!\n" +
                                                 "The email was sent only to addresses before " + r.getError());

                            alert.showAndWait();
                            return;
                        }else {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Internal error");
                            alert.setHeaderText(null);
                            alert.setContentText("Internal error: Try again later!");

                            alert.showAndWait();
                            return;
                        }
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


    public void initController(Mailbox mailbox, HashMap<String, Pane> screenMap, BorderPane root, Requester requester) {
        // ensure model is only set once:
        if (this.mailbox != null) {
            throw new IllegalStateException("Model can only be initialized once");
        }

        this.mailbox = mailbox;
        this.screenMap = screenMap;
        this.root = root;
        this.requester = requester;

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

                if(newMail != null && (newMail.getRecipients() == null || newMail.getRecipients().isEmpty())){
                    System.out.println("Arrivo da una Forward/New");
                    currentRecipientsTextField.setVisible(true);
                    currentRecipientsListView.setVisible(false);
                } else {
                    //System.out.println("Arrivo da una Reply/Reply All: " + newMail.getRecipients());
                    currentRecipientsListView.setMinHeight(newMail.recipientsProperty().size() * LIST_CELL_HEIGHT);
                    currentRecipientsListView.setPrefHeight(newMail.recipientsProperty().size() * LIST_CELL_HEIGHT);
                    recipientsRow.setMinHeight(newMail.recipientsProperty().size() * LIST_CELL_HEIGHT);
                    recipientsRow.setPrefHeight(newMail.recipientsProperty().size() * LIST_CELL_HEIGHT);

                    currentRecipientsTextField.setVisible(false);
                    currentRecipientsListView.setVisible(true);
                }

                if (oldMail != null) {
                    currentTitle.textProperty().unbindBidirectional(oldMail.titleProperty());
                    currentRecipientsTextField.textProperty().unbindBidirectional(oldMail.recipientsProperty());
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
                    currentRecipientsTextField.setText(""); // Capire bene perchè con questo risolvo il bug New into Forward
                    currentRecipientsTextField.focusedProperty().addListener(recipientsListener);
                    currentRecipientsListView.setItems(newMail.recipientsProperty());
                    currentText.textProperty().bindBidirectional(newMail.textProperty());

                    System.out.println("LA NUOVA MAIL: " + mailbox.getCurrentMail());
                }

                System.out.println("CAMBIATO DA NEW: " + mailbox.getCurrentMail());
            }
        });
    }


}
