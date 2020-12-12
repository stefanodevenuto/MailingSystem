package progetto.client.controller;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import progetto.client.model.Mailbox;
import progetto.common.Mail;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class LoginAndMailboxController {

    private Mailbox mailbox;
    private Requester requester;

    private HashMap<String, Pane> screenMap;
    private BorderPane root;

    public static final Pattern EMAIL_ADDRESS_REGEX =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
    public static final int MAX_TRIES = 10;

    @FXML
    private TextField insertedMail;

    @FXML
    private ListView<Mail> mailListView;

    @FXML
    private Button newBtn;

    public void initController(Mailbox mailbox, HashMap<String, Pane> screenMap, BorderPane root, Requester requester) {
        // ensure model is only set once
        if (this.mailbox != null) {
            throw new IllegalStateException("Model can only be initialized once");
        }

        this.mailbox = mailbox;
        this.screenMap = screenMap;
        this.root = root;
        this.requester = requester;
    }

    @FXML
    public void handleNewButton(ActionEvent actionEvent) {
        Mail newMail = new Mail();

        mailbox.setCurrentMail(newMail);

        root.setRight(screenMap.get("newMail"));
    }

    @FXML
    public void handleLoginButton(ActionEvent actionEvent) {

        String givenMailAddress = insertedMail.getText();

        // Set the elements to visualize for every Mail in the ListView
        mailListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            public void updateItem(Mail mail, boolean empty) {
                super.updateItem(mail, empty);
                if (empty) {
                    setText("\n\n\n");
                } else {
                    String newMailCssClass = "new-mail";
                    if(mail.getNewMail()){
                        getStyleClass().add(newMailCssClass);
                    }

                    selectedProperty().addListener((observableValue, aBoolean, notSelected) -> {
                        if (!notSelected) {
                            getStyleClass().remove(newMailCssClass);
                        }
                    });


                    setText("Title: " + mail.getTitle() + "\n" +
                            "From: " + mail.getSender() + "\n" +
                            "Date: " + mail.getDateOfDispatch().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                }
            }
        });

        // To immediately scroll to the bottom
        mailListView.scrollTo(mailListView.getItems().size() - 1);

        requester.getAndUpdateMailList(givenMailAddress, mailListView, newBtn);

    }

    @FXML
    public void handleMouseClicked(MouseEvent arg0) {
        Mail m = mailListView.getSelectionModel().getSelectedItem();
        if(m != null){
            m.setNewMail(false);
            System.out.println("Clicked: " + m.getID() + " " + m.toString());
            m.setNewMail(false);
            mailbox.setCurrentMail(m);


            root.setRight(screenMap.get("singleMail"));
        }
    }

    private static boolean validate(String email) {
        Matcher matcher = EMAIL_ADDRESS_REGEX.matcher(email);
        return matcher.find();
    }
}