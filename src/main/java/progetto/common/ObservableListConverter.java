package progetto.common;

import com.opencsv.bean.AbstractBeanField;
import javafx.beans.property.SimpleStringProperty;

// To parse a CSV file to SimpleProperties
public class ObservableListConverter extends AbstractBeanField {

    @Override
    public Object convert(String value) {
        return new SimpleStringProperty(value);
    }

    @Override
    public String convertToWrite(Object value) {
        return (String)value;
    }

}
