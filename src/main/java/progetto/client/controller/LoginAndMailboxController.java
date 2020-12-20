package progetto.client.controller;

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

public class LoginAndMailboxController {

    private Mailbox mailbox;                                   // The model
    private Requester requester;                               // The instance of the Requester API

    private HashMap<String, Pane> screenMap;                   // References to loaded FXML files
    private BorderPane root;                                   // The main panel
    private NewMailController newMailController;               // The controller of the single mail view
    private SingleMailController singleMailController;         // The controller of the new mail view

    @FXML
    private TextField insertedMail;                            // Where the user insert the email address

    @FXML
    private ListView<Mail> mailListView;                       // Current mail list

    @FXML
    private Button newBtn;                                     // In order to create a new mail

    /**
     * Initialize all the resources needed by the controller to properly operate
     * @param mailbox the model
     * @param screenMap a Map object that contains references to the loaded FXML files
     * @param root the main panel
     * @param requester the instance of the Requester API
     * @param singleMailController the controller of the single mail view
     * @param newMailController the controller of the new mail view
     */
    public void initController(Mailbox mailbox, HashMap<String, Pane> screenMap, BorderPane root, Requester requester,
                               SingleMailController singleMailController, NewMailController newMailController) {
        // ensure model is only set once
        if (this.mailbox != null) {
            throw new IllegalStateException("Model can only be initialized once");
        }

        this.mailbox = mailbox;
        this.screenMap = screenMap;
        this.root = root;
        this.requester = requester;
        this.newMailController = newMailController;
        this.singleMailController= singleMailController;
    }

    @FXML
    public void handleNewButton(ActionEvent actionEvent) {
        // Create a new mail
        Mail newMail = new Mail();
        //newMail.setRecipients(new ArrayList<>());

        mailbox.setCurrentMail(newMail);

        // Show the new mail view
        root.setRight(screenMap.get("newMail"));
        newMailController.show();
    }

    @FXML
    public void handleLoginButton(ActionEvent actionEvent) {

        String givenMailAddress = insertedMail.getText();

        // Set the elements to visualize for every Mail in the ListView
        mailListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            public void updateItem(Mail mail, boolean empty) {
                super.updateItem(mail, empty);
                final String newMailCssClass = "new-mail";
                if (empty) {
                    setText("\n\n\n");
                    getStyleClass().removeAll(newMailCssClass);
                } else {

                    // If it's a new mail add the proper CSS
                    if(mail.getNewMail()){
                        getStyleClass().add(newMailCssClass);
                    } else {
                        getStyleClass().removeAll(newMailCssClass);
                    }

                    // Remove new mail CSS when it loses focus
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

        // Ask the requester to get and continuously update the mail list (and the bound list view)
        requester.getAndUpdateMailList(givenMailAddress, mailListView, newBtn, singleMailController, newMailController);

        // To immediately scroll to the bottom
        mailListView.scrollTo(mailListView.getItems().size() - 1);

    }

    @FXML
    public void handleMouseClicked(MouseEvent click) {
        singleMailController.show();

        Mail m = mailListView.getSelectionModel().getSelectedItem();

        if(m != null){
            System.out.println("Clicked: " + m.getID() + " " + m.toString());

            // Mark it as a read mail
            m.setNewMail(false);

            // Change the current mail
            mailbox.setCurrentMail(m);
            root.setRight(screenMap.get("singleMail"));
        }
    }
}
