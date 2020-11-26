package progetto.client;

import progetto.common.Mail;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.List;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        //Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
        //primaryStage.setTitle("Mailbox");
        //primaryStage.setScene(new Scene(root));
        //primaryStage.show();

        BorderPane root = new BorderPane();

        FXMLLoader loginAndMailboxLoader = new FXMLLoader(getClass().getResource("/progetto.client/loginAndMailbox.fxml"));
        root.setLeft(loginAndMailboxLoader.load());
        LoginAndMailboxController loginAndMailboxController = loginAndMailboxLoader.getController();

        FXMLLoader singleMailLoader = new FXMLLoader(getClass().getResource("/progetto.client/singleMail.fxml"));
        root.setRight(singleMailLoader.load());
        SingleMailController singleMailController = singleMailLoader.getController();

        Mailbox mailbox = new Mailbox();
        loginAndMailboxController.initModel(mailbox);
        singleMailController.initModel(mailbox);

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
        /*try {
            Socket server = new Socket("localhost", 4444);

            try {
                ObjectInputStream inStream = new ObjectInputStream(server.getInputStream());
                System.out.println("2");
                List<Mail> mailList = (List<Mail>) inStream.readObject();
                System.out.println("3");
                for(Mail m : mailList){
                    System.out.println("Title: " + m.getTitle() + ", Text: " + m.getText());
                }

            } finally {
                System.out.println("Chiuso");
                server.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Fine");*/
    }
}
