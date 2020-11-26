package progetto.client;

import progetto.common.Mail;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class SingleMailController {

    private Mailbox mailbox;

    @FXML
    private TextField currentEmail;


    public void initModel(Mailbox mailbox) {
        // ensure model is only set once:
        if (this.mailbox != null) {
            throw new IllegalStateException("Model can only be initialized once");
        }

        this.mailbox = mailbox;

        this.mailbox.currentMailProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldObj, Object newObj) {
                Mail oldMail = (Mail) oldObj;
                Mail newMail = (Mail) newObj;

                if (oldMail != null) {
                    currentEmail.textProperty().unbind();
                }
                if (newMail == null) {
                    currentEmail.setText("");
                } else {
                    currentEmail.textProperty().bind(newMail.textProperty());
                }
            }
        });
    }
}
