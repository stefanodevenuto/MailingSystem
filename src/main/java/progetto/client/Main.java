package progetto.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import progetto.client.controller.LoginAndMailboxController;
import progetto.client.controller.NewMailController;
import progetto.client.controller.Requester;
import progetto.client.controller.SingleMailController;
import progetto.client.model.Mailbox;

import java.util.HashMap;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{

        BorderPane root = new BorderPane();                         // The main panel
        HashMap<String, Pane> screenMap = new HashMap<>();          // References to loaded FXML files

        // Get loaders of needed FXML files
        FXMLLoader loginAndMailboxLoader =
                new FXMLLoader(getClass().getResource("/progetto.client/loginAndMailbox.fxml"));
        FXMLLoader singleMailLoader =
                new FXMLLoader(getClass().getResource("/progetto.client/singleMail.fxml"));
        FXMLLoader newMailLoader =
                new FXMLLoader(getClass().getResource("/progetto.client/newMail.fxml"));

        // Fill the map
        screenMap.put("loginAndMailbox", loginAndMailboxLoader.load());
        screenMap.put("singleMail", singleMailLoader.load());
        screenMap.put("newMail", newMailLoader.load());

        // Starting view
        root.setLeft(screenMap.get("loginAndMailbox"));
        root.setRight(screenMap.get("singleMail"));

        // Get all the controller from the loaders
        LoginAndMailboxController loginAndMailboxController = loginAndMailboxLoader.getController();
        SingleMailController singleMailController = singleMailLoader.getController();
        NewMailController newMailController = newMailLoader.getController();

        // Add custom CSS
        Scene scene = new Scene(root);
        scene.getStylesheets().add(Main.class.getResource("/progetto.client/style.css").toExternalForm());

        // Instantiate a new Model and initialize the controllers
        Mailbox mailbox = new Mailbox();
        Requester requester = new Requester("localhost", 4444, mailbox);

        loginAndMailboxController.initController(mailbox, screenMap, root, requester, singleMailController, newMailController);
        singleMailController.initController(mailbox, screenMap, root, requester, newMailController);
        newMailController.initController(mailbox, requester);

        // Prepare and show thr final view
        primaryStage.setTitle("Client");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) { launch(args); }
}
