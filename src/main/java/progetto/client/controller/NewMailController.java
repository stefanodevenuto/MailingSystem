package progetto.client.controller;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.RowConstraints;
import progetto.client.model.Mailbox;
import progetto.common.Mail;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

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
        mailbox.getCurrentMail().setDateOfDispatch(LocalDate.now());
        requester.sendCurrentMail();
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
                    currentRecipientsTextField.setText("");
                    currentRecipientsTextField.focusedProperty().addListener(recipientsListener);
                    currentRecipientsListView.setItems(newMail.recipientsProperty());
                    currentText.textProperty().bindBidirectional(newMail.textProperty());

                    if(newMail.getRecipients() == null || newMail.getRecipients().isEmpty()){
                        System.out.println("Arrivo da una Forward/New");
                        currentRecipientsTextField.setVisible(true);
                        currentRecipientsListView.setVisible(false);
                    } else {
                        //System.out.println("Arrivo da una Reply/Reply All: " + newMail.getRecipients());
                        IntegerBinding recipientsSize = Bindings.size(newMail.recipientsProperty()).multiply(LIST_CELL_HEIGHT);

                        currentRecipientsListView.minHeightProperty().bind(recipientsSize);
                        currentRecipientsListView.prefHeightProperty().bind(recipientsSize);

                        recipientsRow.minHeightProperty().bind(recipientsSize);
                        recipientsRow.prefHeightProperty().bind(recipientsSize);

                        currentRecipientsTextField.setVisible(false);
                        currentRecipientsListView.setVisible(true);
                    }

                    System.out.println("LA NUOVA MAIL: " + mailbox.getCurrentMail());
                }

                System.out.println("CAMBIATO DA NEW: " + mailbox.getCurrentMail());
            }
        });
    }

    /*private void bindBidirectionalAll(StringProperty titleProperty, StringProperty senderProperty,
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
    }*/

}
