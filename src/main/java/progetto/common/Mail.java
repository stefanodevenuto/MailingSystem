package progetto.common;

import com.opencsv.bean.CsvBindByPosition;
import com.opencsv.bean.CsvCustomBindByPosition;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;

public class Mail implements Externalizable  {

    @CsvBindByPosition(position = 0)
    private int ID;

    @CsvCustomBindByPosition(position = 1, converter = StringPropertyConverter.class)
    private StringProperty title = new SimpleStringProperty();

    @CsvCustomBindByPosition(position = 2, converter = StringPropertyConverter.class)
    private StringProperty text = new SimpleStringProperty();

    @CsvCustomBindByPosition(position = 3, converter = StringPropertyConverter.class)
    private StringProperty sender =  new SimpleStringProperty();

    @CsvCustomBindByPosition(position = 4, converter = ObservableListConverter.class)
    private ObservableList<String> recipients = FXCollections.observableArrayList(new ArrayList<>());

    public Mail() {}

    public Mail(String title, String text, String sender, List<String> recipients) {
        setTitle(title);
        setText(text);
        setSender(sender);
        setRecipients(recipients);
    }

    public StringProperty titleProperty() {
        return this.title;
    }
    public String getTitle() {
        return this.titleProperty().get();
    }
    public void setTitle(String title) {
        this.titleProperty().set(title);
    }

    public StringProperty textProperty() {
        return this.text;
    }
    public String getText() {
        return this.textProperty().get();
    }
    public void setText(String text) {
        this.textProperty().set(text);
    }

    public StringProperty senderProperty() {
        return this.sender;
    }
    public String getSender() {
        return this.senderProperty().get();
    }
    public void setSender(String sender) {
        this.senderProperty().set(sender);
    }

    public ObservableList<String> recipientsProperty() {
        return recipients;
    }
    public void setRecipients(List<String> current) {
        this.recipientsProperty().setAll(current);
    }
    public List<String> getRecipients() {
        return new ArrayList<>(recipientsProperty());
    }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(ID);
        out.writeObject(getTitle());
        out.writeObject(getText());
        out.writeObject(getSender());
        out.writeObject(new ArrayList<>(recipientsProperty()));
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        setID(in.readInt());
        setTitle((String) in.readObject());
        setText((String) in.readObject());
        setSender((String) in.readObject());
        setRecipients((List<String>) in.readObject());
    }

    @Override
    public String toString() {
        return "Title: " + title.get() + "\n" +
                "From: " + sender.get() + "\n";
    }
}
