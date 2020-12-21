package progetto.client.model;

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

    private final SimpleStringProperty address =                              // The current logged user
            new SimpleStringProperty();
    private final ObservableList<Mail> currentMailList =                      // The current mail list
            FXCollections.observableArrayList(new ArrayList<>());
    private final ObjectProperty<Mail> currentMail =                          // The current mail of the mail list
            new SimpleObjectProperty<>(null);


    // Address property usual methods
    public StringProperty addressProperty() { return this.address; }
    public String getAddress() { return this.addressProperty().get(); }
    public void setAddress(String address) { this.addressProperty().set(address); }

    // Current mail list property usual methods
    public ObservableList<Mail> currentMailListProperty() { return currentMailList; }
    /*public synchronized void setCurrentMailList(List<Mail> current) {
        System.out.println("Setting: ");
        for(Mail m : current)
            System.out.print(m + " ");
        currentMailList = FXCollections.observableArrayList(current);
    }*/
    public synchronized void addCurrentMailList(Mail mail){ currentMailList.add(mail); }
    //public List<Mail> getCurrentMailList() { return new ArrayList<>(currentMailListProperty()); }
    public synchronized void removeCurrentMail(){ currentMailList.remove(getCurrentMail()); }
    public synchronized void clearCurrentMailList() { currentMailListProperty().clear(); }
    public synchronized int getSizeCurrentMailList() { return currentMailListProperty().size(); }


    // Current mail property usual methods
    public ObjectProperty<Mail> currentMailProperty() {
        return currentMail;
    }
    public Mail getCurrentMail() { return currentMailProperty().get(); }
    public void setCurrentMail(Mail mail) {
        currentMailProperty().set(mail);
    }

}
