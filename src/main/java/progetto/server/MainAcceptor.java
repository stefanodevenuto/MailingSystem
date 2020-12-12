package progetto.server;

import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import javafx.scene.Scene;
import progetto.common.Mail;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainAcceptor extends Application {
    private static final int MAX_THREADS = 3;
    ExecutorService executors = Executors.newFixedThreadPool(MAX_THREADS);

    @Override
    public void start(Stage primaryStage) throws Exception{
        BorderPane root = new BorderPane();
        FXMLLoader serverLogLoader = new FXMLLoader(getClass().getResource("/progetto.server/logsTableView.fxml"));
        root.setCenter(serverLogLoader.load());

        ServerLogController serverLogController = serverLogLoader.getController();

        Mailboxes mailboxes = new Mailboxes("C:\\Users\\stefa\\Desktop\\users.txt");
        serverLogController.initController(mailboxes, executors);

        Scene scene = new Scene(root);
        primaryStage.setTitle("Server");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        executors.shutdown();
    }

    public static void main(String[] args) { launch(args); }
}
