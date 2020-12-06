package progetto.client;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import progetto.common.Mail;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.List;

public class Mailbox {

    private SimpleStringProperty address = new SimpleStringProperty();
    private ObservableList<Mail> currentMailList = FXCollections.observableArrayList(new ArrayList<>());
    private ObjectProperty<Mail> currentMail = new SimpleObjectProperty<>(null);

    public StringProperty addressProperty() {
        return this.address;
    }
    public String getAddress() {
        return this.addressProperty().get();
    }
    public void setAddress(String address) {
        this.addressProperty().set(address);
    }

    public ObservableList<Mail> currentMailListProperty() {
        return currentMailList;
    }
    public void setCurrentMailList(ObservableList<Mail> current) {
        currentMailList = current;
    }
    public List<Mail> getCurrentMailList() {
        return new ArrayList<>(currentMailListProperty());
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
