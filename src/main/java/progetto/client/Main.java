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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main extends Application {
    private static final int MAX_THREADS = 3;
    ExecutorService executors = Executors.newFixedThreadPool(MAX_THREADS);

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
        Requester requester = new Requester("localhost", 4444, mailbox, executors);

        loginAndMailboxController.initController(mailbox, screenMap, root, requester, singleMailController, newMailController);
        singleMailController.initController(mailbox, screenMap, root, requester, newMailController);
        newMailController.initController(mailbox, requester);

        // Prepare and show thr final view
        primaryStage.setTitle("Client");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        executors.shutdown();
    }

    public static void main(String[] args) { launch(args); }
}
