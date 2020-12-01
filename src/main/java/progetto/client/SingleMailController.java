package progetto.client;

import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.stage.Stage;
import progetto.common.Mail;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;

import java.util.ArrayList;
import java.util.List;

public class SingleMailController {

    private Mailbox mailbox;
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
    public void handleReplyButton(ActionEvent actionEvent) {
        //currentEmail.textProperty().unbind();
        //String newRecipient = mailbox.getCurrentMail().getSender();

        /*BorderPane root = new BorderPane();
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
        }*/
        /*try {
            FXMLLoader newMailLoader = new FXMLLoader(getClass().getResource("/progetto.client/newMail.fxml"));
            root.setRight(newMailLoader.load());
            LoginAndMailboxController newMailController = newMailLoader.getController();
        } catch (Exception e) {
            e.printStackTrace();
        }*/


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
                    currentTitle.textProperty().unbind();
                    currentSender.textProperty().unbind();
                    currentRecipients.setItems(null);
                    currentText.textProperty().unbind();
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
                    currentTitle.textProperty().bind(newMail.titleProperty());
                    currentSender.textProperty().bind(newMail.senderProperty());

                    currentRecipients.setItems(newMail.recipientsProperty());

                    // In order to make the Recipients' ListView list-size tall
                    // TODO: creare un punto dove diventa solo più scrollable
                    currentRecipients.setMinHeight(newMail.recipientsProperty().size() * LIST_CELL_HEIGHT);
                    currentRecipients.setPrefHeight(newMail.recipientsProperty().size() * LIST_CELL_HEIGHT);
                    recipientsRow.setMinHeight(newMail.recipientsProperty().size() * LIST_CELL_HEIGHT);
                    recipientsRow.setPrefHeight(newMail.recipientsProperty().size() * LIST_CELL_HEIGHT);


                    currentText.textProperty().bind(newMail.textProperty());


                }
            }
        });
    }

    private Node getNodeFromGridPane(GridPane gridPane, int col, int row) {
        for (Node node : gridPane.getChildren()) {
            if (GridPane.getColumnIndex(node) == col && GridPane.getRowIndex(node) == row) {
                return node;
            }
        }
        return null;
    }
}
