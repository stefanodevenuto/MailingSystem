package progetto.server;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import progetto.common.Request;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Log {
    private final static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private StringProperty dateTime = new SimpleStringProperty();
    private StringProperty requester = new SimpleStringProperty();
    private ObjectProperty<Request> request = new SimpleObjectProperty<>();
    //private StringProperty status = new SimpleStringProperty();
    private ObjectProperty<ImageView> status = new SimpleObjectProperty<>();

    public Log(String requester, Request request, ImageView image){
        setLocalDateTime(LocalDateTime.now().format(formatter));
        setRequester(requester);
        setRequest(request);
        setStatus(image);
    }

    public StringProperty dateTimeProperty() { return dateTime; }
    public void setLocalDateTime(String localDateTime) { dateTimeProperty().setValue(localDateTime); }
    public String getLocalDateTime() { return dateTimeProperty().get(); }

    public StringProperty requesterProperty() { return requester; }
    public void setRequester(String requester) { requesterProperty().setValue(requester); }
    public String getRequester() { return requesterProperty().get(); }

    public ObjectProperty<Request> requestProperty() { return request; }
    public void setRequest(Request request) { requestProperty().setValue(request); }
    public Request getRequest() { return requestProperty().get(); }

    //public StringProperty statusProperty() { return status; }
    //public void setStatus(String text) { statusProperty().setValue(text); }
    //public String getStatus() { return statusProperty().get(); }

    public ObjectProperty<ImageView> statusProperty() { return status; }
    public void setStatus(ImageView image) { statusProperty().setValue(image); }
    public ImageView getStatus() { return statusProperty().get(); }

}
