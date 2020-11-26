package progetto.client;

import progetto.common.Mail;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class Mailbox {

    private ObservableList<Mail> currentMailList = FXCollections.observableArrayList();
    private ObjectProperty<Mail> currentMail = new SimpleObjectProperty<>(null);

    public ObservableList<Mail> currentMailListProperty() {
        return currentMailList;
    }
    public void setCurrentMailList(ObservableList<Mail> current) {
        currentMailList = current;
    }

    public ObjectProperty<Mail> currentMailProperty() {
        return currentMail;
    }
    public Mail getCurrentMail() {
        return currentMailProperty().get();
    }
    public void setCurrentMail(Mail mail) {
        currentMailProperty().set(mail);
    }

}
