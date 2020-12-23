package progetto.client.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import progetto.common.Mail;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class Mailbox {

    private final SimpleStringProperty address =                              // The current logged user
            new SimpleStringProperty();
    private final ObservableList<Mail> currentMailList =                      // The current mail list
            FXCollections.observableArrayList(new ArrayList<>());
    private final ObjectProperty<Mail> currentMail =                          // The current mail of the mail list
            new SimpleObjectProperty<>(null);

    private final AtomicBoolean connected = new AtomicBoolean(false);

    // Address property usual methods
    public StringProperty addressProperty() { return this.address; }
    public String getAddress() { return this.addressProperty().get(); }
    public void setAddress(String address) { this.addressProperty().set(address); }

    // Current mail list property usual methods
    public ObservableList<Mail> currentMailListProperty() { return currentMailList; }
    public synchronized void addCurrentMailList(Mail mail){ currentMailList.add(mail); }
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

    // Connected setter/getter
    public void setConnected(boolean connected) {
        this.connected.set(connected);
    }
    public boolean getConnected() {
        return connected.get();
    }
}
