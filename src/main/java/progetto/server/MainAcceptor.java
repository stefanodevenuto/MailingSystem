package progetto.server;

import javafx.scene.Scene;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainAcceptor extends Application {
    private static final int MAX_THREADS = 3;
    ExecutorService executors = Executors.newFixedThreadPool(MAX_THREADS);
    ServerLogController.StartListener startListener;

    @Override
    public void start(Stage primaryStage) throws Exception{
        BorderPane root = new BorderPane();                 // The main panel

        // Recover loader of needed fxml file
        FXMLLoader serverLogLoader =
                new FXMLLoader(getClass().getResource("/server/logsTableView.fxml"));
        root.setCenter(serverLogLoader.load());

        // Get the controller from the loader
        ServerLogController serverLogController = serverLogLoader.getController();

        // Instantiate a new Model
        Mailboxes mailboxes = new Mailboxes("src\\main\\java\\progetto\\server\\mailboxes\\");

        // Initialize the controller with the proper information and return the listener thread
        startListener = serverLogController.initController(mailboxes, executors);

        // Prepare and show thr final view
        Scene scene = new Scene(root);
        primaryStage.setTitle("Server");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        startListener.closeSocket();    // Close the socket for incoming requests
        executors.shutdown();           // Shutdown the executor service (no new requests)
    }

    public static void main(String[] args) { launch(args); }
}
