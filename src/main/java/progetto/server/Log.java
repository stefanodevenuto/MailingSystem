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
    private final static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd, HH:mm:ss");

    private final StringProperty dateTime = new SimpleStringProperty();
    private final StringProperty requester = new SimpleStringProperty();
    private final ObjectProperty<Request> request = new SimpleObjectProperty<>();
    private final ObjectProperty<Image> status = new SimpleObjectProperty<>();
    private final StringProperty statusText = new SimpleStringProperty();

    public Log(String requester, Request request, Image image){
        setLocalDateTime(LocalDateTime.now().format(formatter));
        setRequester(requester);
        setRequest(request);
        setStatus(image);
        setStatusText("Elaborating...");
    }

    // Arrival time of request property usual methods
    public StringProperty dateTimeProperty() { return dateTime; }
    public void setLocalDateTime(String localDateTime) { dateTimeProperty().setValue(localDateTime); }
    public String getLocalDateTime() { return dateTimeProperty().get(); }

    // Requester property usual methods
    public StringProperty requesterProperty() { return requester; }
    public void setRequester(String requester) { requesterProperty().setValue(requester); }
    public String getRequester() { return requesterProperty().get(); }

    // Request property usual methods
    public ObjectProperty<Request> requestProperty() { return request; }
    public void setRequest(Request request) { requestProperty().setValue(request); }
    public Request getRequest() { return requestProperty().get(); }

    // Status text property usual methods
    public StringProperty statusTextProperty() { return statusText; }
    public void setStatusText(String text) { statusTextProperty().setValue(text); }
    public String getStatusText() { return statusTextProperty().get(); }

    // Status image property usual methods
    public ObjectProperty<Image> statusProperty() { return status; }
    public void setStatus(Image image) { status.set(image); }
    public Image getStatus() { return status.get(); }

}
