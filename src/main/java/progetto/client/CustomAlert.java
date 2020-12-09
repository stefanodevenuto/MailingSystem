package progetto.client;

import javafx.scene.control.Alert;

public class CustomAlert {
    public static Alert informationAlert(String title){
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);

        return alert;
    }

    public static Alert errorAlert(String title){
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);

        return alert;
    }

    public static Alert warningAlert(String title){
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);

        return alert;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static Alert wrongAddress(String address){
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Wrong Address");
        alert.setHeaderText(null);
        alert.setContentText("The inserted " + address + " address is not present!");

        return alert;
    }

    public static Alert internalError(){
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Internal Error");
        alert.setHeaderText(null);
        alert.setContentText("There's an internal server error: try again later!");

        return alert;
    }
}
