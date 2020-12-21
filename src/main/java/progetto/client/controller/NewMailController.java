package progetto.client.controller;

import javafx.animation.Transition;
import javafx.animation.TranslateTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.transform.Translate;
import javafx.util.Duration;
import progetto.client.model.Mailbox;
import progetto.common.Mail;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class NewMailController {
    private Mailbox mailbox;                                    // The model
    private Requester requester;                                // The instance of the Requester API

    private TranslateTransition hideGridPane;                   // The send mail animation
    private boolean showed = true;                              // Reveal the state of the view

    private static final int LIST_CELL_HEIGHT = 24;             // The height of a single list row

    @FXML
    private GridPane gridPane;                                  // Main grid pane

    @FXML
    private TextField currentTitle;                             // Title of the new mail

    @FXML
    private TextField currentRecipientsTextField;               // Recipients text field of the new mail

    @FXML
    private ListView<String> currentRecipientsListView;         // Recipients list view of the new mail

    @FXML
    private RowConstraints recipientsRow;                       // Grid pane row constraint of the recipient one

    @FXML
    private TextArea currentText;                               // Text of the new mail


    /**
     * Initialize all the resources needed by the controller to properly operate
     * @param mailbox the model
     * @param requester the instance of the Requester API
     */
    public void initController(Mailbox mailbox, Requester requester) {
        // ensure model is only set once:
        if (this.mailbox != null) {
            throw new IllegalStateException("Model can only be initialized once");
        }

        this.mailbox = mailbox;
        this.requester = requester;

        // Change listener that split mail addresses by semicolon (;)
        ChangeListener<Boolean> recipientsListener = (observable, oldProperty, newProperty) -> {
            if(!newProperty){
                String givenRecipients = currentRecipientsTextField.getText();
                String[] recipients = givenRecipients.split(";");

                mailbox.getCurrentMail().setRecipients(new ArrayList<>(Arrays.asList(recipients)));

                System.out.println(Arrays.toString(recipients));
            }
        };

        this.mailbox.currentMailProperty().addListener(new ChangeListener<Mail>() {
            @Override
            public void changed(ObservableValue observable, Mail oldMail, Mail newMail) {

                // Unbind all if the old mail doesn't exist / doesn't exist anymore
                if (oldMail != null) {
                    currentTitle.textProperty().unbindBidirectional(oldMail.titleProperty());
                    currentRecipientsTextField.textProperty().unbindBidirectional(oldMail.recipientsProperty());
                    currentRecipientsTextField.focusedProperty().removeListener(recipientsListener);
                    currentRecipientsListView.setItems(null);
                    currentText.textProperty().unbindBidirectional(oldMail.textProperty());
                }

                // If a new mail doesn't exist
                if (newMail == null) {
                    currentTitle.setText("");
                    currentRecipientsTextField.setText("");
                    currentRecipientsListView.setItems(null);
                    currentText.setText("");

                } else {

                    // Bind all to the new mail
                    currentTitle.textProperty().bindBidirectional(newMail.titleProperty());
                    currentRecipientsTextField.setText("");
                    currentRecipientsTextField.focusedProperty().addListener(recipientsListener);
                    currentRecipientsListView.setItems(newMail.recipientsProperty());
                    currentText.textProperty().bindBidirectional(newMail.textProperty());

                    // Make the recipients' list view visible if Forward/New button was pressed, otherwise make visible
                    // the recipients' text field (Reply/Reply All button pressed)
                    if(newMail.getRecipients() == null || newMail.getRecipients().isEmpty()){
                        currentRecipientsTextField.setVisible(true);
                        currentRecipientsListView.setVisible(false);

                    } else {

                        // Custom bind to resize the list view size to the number of recipients
                        IntegerBinding recipientsSize = Bindings.size(newMail.recipientsProperty()).multiply(LIST_CELL_HEIGHT);

                        currentRecipientsListView.minHeightProperty().bind(recipientsSize);
                        currentRecipientsListView.prefHeightProperty().bind(recipientsSize);

                        recipientsRow.minHeightProperty().bind(recipientsSize);
                        recipientsRow.prefHeightProperty().bind(recipientsSize);

                        currentRecipientsTextField.setVisible(false);
                        currentRecipientsListView.setVisible(true);
                    }
                }
            }
        });

        // Create the transition for the "slide right" animation on Send
        hideGridPane = new TranslateTransition(Duration.millis(250), gridPane);
        hideGridPane.setByX(800.0);
        hideGridPane.setOnFinished(event -> showed = false);
    }

    @FXML
    public void handleSendButton(ActionEvent actionEvent) {
        // Set the sender and the date of dispatch of the new mail
        mailbox.getCurrentMail().setSender(mailbox.getAddress());
        mailbox.getCurrentMail().setDateOfDispatch(LocalDate.now());

        // Ask the requester to send the current mail
        requester.sendCurrentMail(this);
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

}
