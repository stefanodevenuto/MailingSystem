package progetto.client;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.RowConstraints;
import javafx.stage.Stage;
import progetto.common.Mail;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import progetto.common.Request;
import progetto.common.Response;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SingleMailController {

    private Mailbox mailbox;
    private HashMap<String, Pane> screenMap;
    private BorderPane root;
    private static final int LIST_CELL_HEIGHT = 24;

    @FXML
    private GridPane gridPane;

    @FXML
    private TextField currentTitle;

    @FXML
    private TextField currentSender;

    @FXML
    private ListView<String> currentRecipients;

    @FXML
    private RowConstraints recipientsRow;

    @FXML
    private TextArea currentText;

    @FXML
    private Button replyBtn;

    @FXML
    private Button replyAllBtn;

    @FXML
    private Button forwardBtn;

    @FXML
    private Button deleteBtn;

    @FXML
    private Button sendBtn;

    @FXML
    public void handleReplyButton(ActionEvent actionEvent) {

        List<String> newRecipient = new ArrayList<>();
        newRecipient.add(mailbox.getCurrentMail().getSender());

        Mail m = new Mail();
        m.setRecipients(newRecipient);

        mailbox.setCurrentMail(m);

        root.setRight(screenMap.get("newMail"));
        System.out.println("Sto settando come recipients: " + newRecipient);

    }

    @FXML
    public void handleReplyAllButton(ActionEvent actionEvent) {

        List<String> newRecipients = mailbox.getCurrentMail().getRecipients();

        newRecipients.remove(mailbox.getAddress());
        if(!newRecipients.contains(mailbox.getCurrentMail().getSender()))
            newRecipients.add(mailbox.getCurrentMail().getSender());

        Mail m = new Mail();
        m.setRecipients(newRecipients);

        mailbox.setCurrentMail(m);

        root.setRight(screenMap.get("newMail"));
        System.out.println("Sto settando come recipients: " + newRecipients);

    }

    @FXML
    public void handleForwardButton(ActionEvent actionEvent){
        Mail newMail = new Mail();

        newMail.setText(mailbox.getCurrentMail().getText());
        //newMail.setRecipients(null);

        mailbox.setCurrentMail(newMail);

        root.setRight(screenMap.get("newMail"));
    }

    public void initController(Mailbox mailbox, HashMap<String, Pane> screenMap, BorderPane root) {
        // ensure model is only set once:
        if (this.mailbox != null) {
            throw new IllegalStateException("Model can only be initialized once");
        }

        this.mailbox = mailbox;
        this.screenMap = screenMap;
        this.root = root;

        this.mailbox.currentMailProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldObj, Object newObj) {
                Mail oldMail = (Mail) oldObj;
                Mail newMail = (Mail) newObj;

                if (oldMail != null) {
                    unbindAll();
                }
                if (newMail == null) {
                    currentTitle.setText("");
                    currentSender.setText("");
                    currentRecipients.setItems(null);
                    currentText.setText("");

                    // In order to make them not visible when an empty MailList arrive
                    gridPane.setVisible(false);
                } else {
                    gridPane.setVisible(true);

                    bindAll(newMail.titleProperty(), newMail.senderProperty(),
                            newMail.recipientsProperty(), newMail.textProperty());

                    // In order to make the Recipients' ListView list-length tall
                    // TODO: creare un punto dove diventa solo più scrollable
                    currentRecipients.setMinHeight(newMail.recipientsProperty().size() * LIST_CELL_HEIGHT);
                    currentRecipients.setPrefHeight(newMail.recipientsProperty().size() * LIST_CELL_HEIGHT);
                    recipientsRow.setMinHeight(newMail.recipientsProperty().size() * LIST_CELL_HEIGHT);
                    recipientsRow.setPrefHeight(newMail.recipientsProperty().size() * LIST_CELL_HEIGHT);

                }
            }
        });
    }

    private void bindAll(StringProperty titleProperty, StringProperty senderProperty,
                         ObservableList<String> recipientsProperty, StringProperty textProperty){

        currentTitle.textProperty().bind(titleProperty);
        currentSender.textProperty().bind(senderProperty);
        currentRecipients.setItems(recipientsProperty);
        currentText.textProperty().bind(textProperty);
    }

    private void unbindAll(){
        currentTitle.textProperty().unbind();
        currentSender.textProperty().unbind();
        currentRecipients.setItems(null);
        currentText.textProperty().unbind();
    }

    private void bindBidirectionalAll(StringProperty titleProperty, StringProperty senderProperty,
                                      ObservableList<String> recipientsProperty, StringProperty textProperty){

        currentTitle.textProperty().bindBidirectional(titleProperty);
        currentSender.textProperty().bindBidirectional(senderProperty);
        currentRecipients.setItems(recipientsProperty);
        currentText.textProperty().bindBidirectional(textProperty);

    }

    private void unbindBidirectionalAll(StringProperty titleProperty, StringProperty senderProperty,
                                        StringProperty textProperty){

        currentTitle.textProperty().unbindBidirectional(titleProperty);
        currentSender.textProperty().unbindBidirectional(senderProperty);
        currentRecipients.setItems(null);
        currentText.textProperty().unbindBidirectional(textProperty);
    }
}
