package progetto.client;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import progetto.common.Mail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NewMailController {
    private Mailbox mailbox;

    @FXML
    private TextField currentTitle;

    @FXML
    private TextField currentSender;

    @FXML
    private TextField currentRecipients;

    @FXML
    private TextArea currentText;

    public void initModel(Mailbox mailbox) {
        // ensure model is only set once:
        if (this.mailbox != null) {
            throw new IllegalStateException("Model can only be initialized once");
        }

        this.mailbox = mailbox;

        mailbox.setCurrentMail(new Mail());

        ChangeListener<String> recipientsListener = new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldObj, Object newObj) {
                if(newObj != null && newObj instanceof String){
                    String givenRecipients = (String) newObj;
                    String[] recipients = givenRecipients.split(";");

                    mailbox.getCurrentMail().setRecipients(new ArrayList<>(Arrays.asList(recipients)));
                }
            }
        };

        this.mailbox.currentMailProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldObj, Object newObj) {
                Mail oldMail = (Mail) oldObj;
                Mail newMail = (Mail) newObj;

                if (oldMail != null) {
                    currentTitle.textProperty().unbindBidirectional(oldMail.titleProperty());
                    currentSender.textProperty().unbindBidirectional(oldMail.senderProperty());
                    currentRecipients.textProperty().removeListener(recipientsListener);
                    currentText.textProperty().unbindBidirectional(oldMail.textProperty());
                }
                if (newMail == null) {
                    currentTitle.setText("");
                    currentSender.setText("");
                    currentRecipients.setText("");
                    currentText.setText("");

                } else {
                    currentTitle.textProperty().bindBidirectional(newMail.titleProperty());
                    currentSender.textProperty().bindBidirectional(newMail.senderProperty());
                    currentRecipients.textProperty().addListener(recipientsListener);
                    currentText.textProperty().bindBidirectional(newMail.textProperty());
                }
            }
        });
    }


}
