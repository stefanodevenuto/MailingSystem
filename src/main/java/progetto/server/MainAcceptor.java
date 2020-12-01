package progetto.server;

import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
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

public class MainAcceptor extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        BorderPane root = new BorderPane();

        FXMLLoader logServerLoader = new FXMLLoader(getClass().getResource("/progetto.server/logServer.fxml"));
        root.setCenter(logServerLoader.load());
        ServerLogController logServerController = logServerLoader.getController();

        //GlobalMailbox globalMailbox = new GlobalMailbox();
        //logServerController.initModel();
    }

    public static void main(String[] args) {

        // TODO: da rimuovere, solo per provare
        try {

            Writer writer = Files.newBufferedWriter(Paths.get("C:\\Users\\stefa\\Desktop\\first@gmail.com.csv"));

            StatefulBeanToCsv<Mail> beanToCsv = new StatefulBeanToCsvBuilder<Mail>(writer)
                    //.withQuotechar(CSVWriter.NO_QUOTE_CHARACTER)
                    .build();

            List<Mail> mails = new ArrayList<>();
            List<String> recipientsOne = new ArrayList<>();

            recipientsOne.add("recipient1");
            recipientsOne.add("recipient2");
            recipientsOne.add("recipient3");

            List<String> recipientsTwo = new ArrayList<>();

            recipientsTwo.add("recipient1");
            recipientsTwo.add("recipient2");

            List<String> recipientsThree = new ArrayList<>();

            recipientsThree.add("recipient1");
            recipientsThree.add("recipient2");
            recipientsThree.add("recipient3");
            recipientsThree.add("recipient4");
            recipientsThree.add("recipient5");

            mails.add(new Mail("title1", "text1,\"ciao", "second@gmail.com", recipientsOne));
            mails.add(new Mail("title2", "text2", "second@gmail.com", recipientsTwo));
            mails.add(new Mail("title3", "text3", "second@gmail.com", recipientsThree));

            beanToCsv.write(mails);
            writer.close();

        } catch(Exception e) {
            e.printStackTrace();
        }
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        launch(args);
    }
}
