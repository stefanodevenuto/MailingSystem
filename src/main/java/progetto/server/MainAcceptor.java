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
        System.out.println("1");

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
            List<String> recipients = new ArrayList<>();

            recipients.add("recipient1");
            recipients.add("recipient2");
            recipients.add("recipient3");

            mails.add(new Mail("title1", "text1,\"ciao", recipients));
            mails.add(new Mail("title2", "text2", recipients));
            mails.add(new Mail("title3", "text3", recipients));

            beanToCsv.write(mails);
            writer.close();

        } catch(Exception e) {
            e.printStackTrace();
        }
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        launch(args);
    }
}
