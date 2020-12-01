package progetto.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Main extends Application {
    ScheduledExecutorService getMailListFixedTime = Executors.newScheduledThreadPool(1);

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
        Node singleMailNode = singleMailLoader.load();
        root.setRight(singleMailNode);
        SingleMailController singleMailController = singleMailLoader.getController();

        // Css
        Scene scene = new Scene(root);
        scene.getStylesheets().add(Main.class.getResource("/progetto.client/singleMail.css").toExternalForm());

        // New Model
        Mailbox mailbox = new Mailbox();
        loginAndMailboxController.initModelAndScene(mailbox, singleMailNode, getMailListFixedTime);
        singleMailController.initModel(mailbox);

        primaryStage.setTitle("Client");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    @Override
    public void stop(){
        getMailListFixedTime.shutdown();
    }


    public static void main(String[] args) {
        launch(args);
        /*try {
            Socket server = new Socket("localhost", 4444);

            try {
                ObjectOutputStream outStream = new ObjectOutputStream(server.getOutputStream());
                ObjectInputStream inStream = new ObjectInputStream(server.getInputStream());
                System.out.println("2");
                outStream.writeObject(new Request(Request.GET_MAILLIST, "first@gmail.com"));
                List<Mail> mailList = (List<Mail>) inStream.readObject();
                System.out.println("3");
                for(Mail m : mailList){
                    System.out.println("Title: " + m.getTitle() + ", Text: " + m.getText());
                    System.out.print("Recipients: ");
                    for(String b : new ArrayList<>(m.recipientsProperty())){
                        System.out.println(b);
                    }
                    System.out.println();
                }

            } finally {
                System.out.println("Chiuso");
                server.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }*/

        System.out.println("Fine");
    }
}
