package progetto.client;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import progetto.common.Mail;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class SingleMailController {

    private Mailbox mailbox;

    @FXML
    private TextField currentEmail;

    @FXML
    private Button replyBtn;

    @FXML
    private Button replyAllBtn;

    @FXML
    private Button forwardBtn;

    @FXML
    public void handleReplyButton(ActionEvent actionEvent) {
        //currentEmail.textProperty().unbind();
        //String newRecipient = mailbox.getCurrentMail().getSender();

        BorderPane root = new BorderPane();
        try {
            FXMLLoader newMailLoader = new FXMLLoader(getClass().getResource("/progetto.client/newMail.fxml"));
            root.setLeft(newMailLoader.load());
            LoginAndMailboxController newMailController = newMailLoader.getController();

            Stage stage = new Stage();
            stage.setTitle("New Mail");
            stage.setScene(new Scene(root));
            stage.show();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

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

                    // In order to make them not visible when an empty MailList arrive
                    currentEmail.setVisible(false);
                    replyBtn.setVisible(false);
                    replyAllBtn.setVisible(false);
                    forwardBtn.setVisible(false);
                } else {
                    currentEmail.textProperty().bind(newMail.textProperty());

                    currentEmail.setVisible(true);
                    replyBtn.setVisible(true);
                    replyAllBtn.setVisible(true);
                    forwardBtn.setVisible(true);
                }
            }
        });
    }
}
