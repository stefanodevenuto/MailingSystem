package progetto.client.controller;

import javafx.animation.TranslateTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.transform.Translate;
import javafx.util.Duration;
import progetto.client.model.Mailbox;
import progetto.common.Mail;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SingleMailController {

    private Mailbox mailbox;                                    // The model
    private Requester requester;                                // The instance of the Requester API

    private HashMap<String, Pane> screenMap;                    // References to loaded FXML files
    private BorderPane root;                                    // The main panel
    private static final int LIST_CELL_HEIGHT = 24;             // The height of a single list row

    private TranslateTransition hideGridPane;                   // The delete mail animation
    private boolean showed = true;                              // Reveal the state of the view
    private NewMailController newMailController;                // The controller of the single mail view

    @FXML
    private GridPane gridPane;                                  // Main grid pane

    @FXML
    private TextField currentTitle;                             // Title of the current mail

    @FXML
    private TextField currentSender;                            // Sender of the current mail

    @FXML
    private ListView<String> currentRecipients;                 // Recipients of the current mail

    @FXML
    private RowConstraints recipientsRow;                       // Grid pane row constraint of the recipient one

    @FXML
    private TextArea currentText;                               // Text of the current mail

    @FXML
    public void handleDeleteButton(ActionEvent actionEvent) {
        // Ask the requester to delete the current mail and start the animation
        requester.deleteCurrentMail(this);
    }

    @FXML
    public void handleReplyButton(ActionEvent actionEvent) {

        // Create recipient of the new mail as the sender of the current mail
        List<String> newRecipient = new ArrayList<>();
        newRecipient.add(mailbox.getCurrentMail().getSender());

        // Create the new mail and set the recipient
        Mail m = new Mail();
        m.setRecipients(newRecipient);

        mailbox.setCurrentMail(m);

        // Show the new mail view
        root.setRight(screenMap.get("newMail"));
        newMailController.show();
    }

    @FXML
    public void handleReplyAllButton(ActionEvent actionEvent) {

        // Create recipients of the new mail as the sender + recipients  of the current mail, excluding the user itself
        List<String> newRecipients = mailbox.getCurrentMail().getRecipients();
        newRecipients.remove(mailbox.getAddress());
        if(!newRecipients.contains(mailbox.getCurrentMail().getSender()))
            newRecipients.add(mailbox.getCurrentMail().getSender());

        // Create the new mail and set the recipients
        Mail m = new Mail();
        m.setRecipients(newRecipients);

        mailbox.setCurrentMail(m);

        // Show the new mail view
        root.setRight(screenMap.get("newMail"));
        newMailController.show();
    }

    @FXML
    public void handleForwardButton(ActionEvent actionEvent){
        // Create the new mail and set the text as the text of current mail
        Mail m = new Mail();
        m.setText(mailbox.getCurrentMail().getText());
        //m.setRecipients(new ArrayList<>());

        mailbox.setCurrentMail(m);

        // Show the new mail view
        root.setRight(screenMap.get("newMail"));
        newMailController.show();
    }

    public void initController(Mailbox mailbox, HashMap<String, Pane> screenMap, BorderPane root,
                               Requester requester, NewMailController newMailController) {
        // ensure model is only set once:
        if (this.mailbox != null) {
            throw new IllegalStateException("Model can only be initialized once");
        }

        this.mailbox = mailbox;
        this.screenMap = screenMap;
        this.root = root;
        this.requester = requester;
        this.newMailController = newMailController;

        this.mailbox.currentMailProperty().addListener(new ChangeListener<Mail>() {
            @Override
            public void changed(ObservableValue observable, Mail oldMail, Mail newMail) {

                // Unbind all if the old mail doesn't exist / doesn't exist anymore
                if (oldMail != null) {
                    unbindAll();
                }

                // If a new mail doesn't exist
                if (newMail == null) {
                    currentTitle.setText("");
                    currentSender.setText("");
                    currentRecipients.setItems(null);
                    currentText.setText("");

                    // In order to make them not visible when an empty MailList arrive
                    gridPane.setVisible(false);
                } else {
                    // Custom bind to resize the list view size to the number of recipients
                    IntegerBinding recipientsSize = Bindings.size(newMail.recipientsProperty()).multiply(LIST_CELL_HEIGHT);

                    currentRecipients.minHeightProperty().bind(recipientsSize);
                    currentRecipients.prefHeightProperty().bind(recipientsSize);
                    recipientsRow.minHeightProperty().bind(recipientsSize);
                    recipientsRow.prefHeightProperty().bind(recipientsSize);

                    // Bind all properties to the new mail
                    bindAll(newMail.titleProperty(), newMail.senderProperty(),
                            newMail.recipientsProperty(), newMail.textProperty());

                    gridPane.setVisible(true);
                }
            }
        });

        // Create the transition for the "slide right" animation on Delete
        hideGridPane = new TranslateTransition(Duration.millis(250), gridPane);
        hideGridPane.setByX(800.0);
        hideGridPane.setOnFinished(event -> showed = false);
    }

    // Make the grid pane visible
    public void show(){
        System.out.println("Showed show: " + showed);
        if(!showed){
            gridPane.setTranslateX(0);
            gridPane.setVisible(true);
            showed = true;
        }
    }

    // Make the grid pane invisible with animation
    public void hide(){
        System.out.println("Showed hide: " + showed);
        if(showed){
            hideGridPane.play();
        }
    }

    // Useful method to bind all properties at once
    private void bindAll(StringProperty titleProperty, StringProperty senderProperty,
                         ObservableList<String> recipientsProperty, StringProperty textProperty){

        currentTitle.textProperty().bind(titleProperty);
        currentSender.textProperty().bind(senderProperty);
        currentRecipients.setItems(recipientsProperty);
        currentText.textProperty().bind(textProperty);
    }

    // Useful method to unbind all properties at once
    private void unbindAll(){
        currentTitle.textProperty().unbind();
        currentSender.textProperty().unbind();
        currentRecipients.setItems(null);
        currentText.textProperty().unbind();
    }
}
