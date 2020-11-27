package progetto.common;

import com.opencsv.bean.CsvCustomBindByPosition;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.List;

public class Mail implements Externalizable {
    //private UUID ID;
    //private SimpleStringProperty sender =  new SimpleStringProperty();

    @CsvCustomBindByPosition(position = 0, converter = StringPropertyConverter.class)
    private StringProperty title = new SimpleStringProperty();

    @CsvCustomBindByPosition(position = 1, converter = StringPropertyConverter.class)
    private StringProperty text = new SimpleStringProperty();

    @CsvCustomBindByPosition(position = 2, converter = ObservableListConverter.class)
    private ObservableList<String> recipients = FXCollections.observableArrayList();

    public Mail() {}

    public Mail(String title, String text, List<String> recipients) {
        //ID = UUID.randomUUID();
        //setSender(sender);
        setTitle(title);
        setText(text);
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

    public ObservableList<String> recipientsProperty() {
        return recipients;
    }
    public void setRecipients(List<String> current) {
        recipients.setAll(current);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(getTitle());
        out.writeObject(getText());
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        setTitle((String) in.readObject());
        setText((String) in.readObject());
    }

}
