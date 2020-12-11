package progetto.server;

import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import javafx.scene.Scene;
import progetto.common.Mail;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainAcceptor extends Application {
    private static final int MAX_THREADS = 3;
    ExecutorService executors = Executors.newFixedThreadPool(MAX_THREADS);

    @Override
    public void start(Stage primaryStage) throws Exception{
        BorderPane root = new BorderPane();
        FXMLLoader serverLogLoader = new FXMLLoader(getClass().getResource("/progetto.server/logServer.fxml"));
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
        executors.shutdown();
        super.stop();
    }

    public static void main(String[] args) {
        // TODO: da rimuovere, solo per provare
        /*try {

            Writer writer = Files.newBufferedWriter(Paths.get("C:\\Users\\stefa\\Desktop\\first@gmail.com.csv"));

            StatefulBeanToCsv<Mail> beanToCsv = new StatefulBeanToCsvBuilder<Mail>(writer)
                    //.withQuotechar(CSVWriter.NO_QUOTE_CHARACTER)
                    .build();

            List<Mail> mails = new ArrayList<>();
            List<String> recipientsOne = new ArrayList<>();

            recipientsOne.add("first@gmail.com");
            recipientsOne.add("second@gmail.com");
            recipientsOne.add("third@gmail.com");

            List<String> recipientsTwo = new ArrayList<>();

            recipientsTwo.add("first@gmail.com");
            recipientsTwo.add("second@gmail.com");

            List<String> recipientsThree = new ArrayList<>();

            recipientsThree.add("first@gmail.com");
            recipientsThree.add("third@gmail.com");
            recipientsThree.add("fourth@gmail.com");
            recipientsThree.add("fifth@gmail.com");

            mails.add(new Mail("title1", "text1,\"ciao", "second@gmail.com", recipientsOne));
            mails.add(new Mail("title2", "text2", "second@gmail.com", recipientsTwo));
            mails.add(new Mail("title3", "text3", "second@gmail.com", recipientsThree));

            beanToCsv.write(mails);
            writer.close();

        } catch(Exception e) {
            e.printStackTrace();
        }*/
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        launch(args);
    }
}
