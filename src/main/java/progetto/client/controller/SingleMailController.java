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

    private Mailbox mailbox;
    private Requester requester;

    private HashMap<String, Pane> screenMap;
    private BorderPane root;
    private static final int LIST_CELL_HEIGHT = 24;

    private TranslateTransition hideGridPane;
    private boolean showed = true;
    private NewMailController newMailController;

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
    public void handleDeleteButton(ActionEvent actionEvent) {
        requester.deleteCurrentMail(this);
    }

    @FXML
    public void handleReplyButton(ActionEvent actionEvent) {

        List<String> newRecipient = new ArrayList<>();
        newRecipient.add(mailbox.getCurrentMail().getSender());

        Mail m = new Mail();
        m.setRecipients(newRecipient);

        mailbox.setCurrentMail(m);

        root.setRight(screenMap.get("newMail"));
        newMailController.show();
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
        newMailController.show();
        System.out.println("Sto settando come recipients: " + newRecipients);

    }

    @FXML
    public void handleForwardButton(ActionEvent actionEvent){
        Mail m = new Mail();

        m.setText(mailbox.getCurrentMail().getText());
        m.setRecipients(new ArrayList<>());

        mailbox.setCurrentMail(m);

        root.setRight(screenMap.get("newMail"));
        newMailController.show();
    }

    public void initController(Mailbox mailbox, HashMap<String, Pane> screenMap, BorderPane root, Requester requester, NewMailController newMailController) {
        // ensure model is only set once:
        if (this.mailbox != null) {
            throw new IllegalStateException("Model can only be initialized once");
        }

        this.mailbox = mailbox;
        this.screenMap = screenMap;
        this.root = root;
        this.requester = requester;
        this.newMailController = newMailController;

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
                    IntegerBinding recipientsSize = Bindings.size(newMail.recipientsProperty()).multiply(LIST_CELL_HEIGHT);

                    bindAll(newMail.titleProperty(), newMail.senderProperty(),
                            newMail.recipientsProperty(), newMail.textProperty());

                    // In order to make the Recipients' ListView list-length tall
                    currentRecipients.minHeightProperty().bind(recipientsSize);
                    currentRecipients.prefHeightProperty().bind(recipientsSize);
                    recipientsRow.minHeightProperty().bind(recipientsSize);
                    recipientsRow.prefHeightProperty().bind(recipientsSize);

                }
            }
        });

        hideGridPane = new TranslateTransition(Duration.millis(250), gridPane);
        hideGridPane.setByX(800.0);
        hideGridPane.setOnFinished(event -> showed = false);
    }

    public void show(){
        System.out.println("Showed show: " + showed);
        if(!showed){
            gridPane.setTranslateX(0);
            showed = true;
        }
    }

    public void hide(){
        System.out.println("Showed hide: " + showed);
        if(showed){
            hideGridPane.play();
        }
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
}
