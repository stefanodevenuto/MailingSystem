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

        BorderPane root = new BorderPane();
        HashMap<String, Pane> screenMap = new HashMap<>();

        FXMLLoader loginAndMailboxLoader = new FXMLLoader(getClass().getResource("/progetto.client/loginAndMailbox.fxml"));
        FXMLLoader singleMailLoader = new FXMLLoader(getClass().getResource("/progetto.client/singleMail.fxml"));
        FXMLLoader newMailLoader = new FXMLLoader(getClass().getResource("/progetto.client/newMail.fxml"));

        screenMap.put("loginAndMailbox", loginAndMailboxLoader.load());
        screenMap.put("singleMail", singleMailLoader.load());
        screenMap.put("newMail", newMailLoader.load());

        root.setLeft(screenMap.get("loginAndMailbox"));
        root.setRight(screenMap.get("singleMail"));

        LoginAndMailboxController loginAndMailboxController = loginAndMailboxLoader.getController();
        SingleMailController singleMailController = singleMailLoader.getController();
        NewMailController newMailController = newMailLoader.getController();

        // Css
        Scene scene = new Scene(root);
        scene.getStylesheets().add(Main.class.getResource("/progetto.client/style.css").toExternalForm());

        // Instantiate a new Model and initialize the controllers
        Mailbox mailbox = new Mailbox();
        Requester requester = new Requester("localhost", 4444, mailbox);

        loginAndMailboxController.initController(mailbox, screenMap, root, requester, singleMailController, newMailController);
        singleMailController.initController(mailbox, screenMap, root, requester, newMailController);
        newMailController.initController(mailbox, screenMap, root, requester);

        primaryStage.setTitle("Client");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) { launch(args); }
}
