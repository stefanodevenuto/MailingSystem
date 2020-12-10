package progetto.common.converters;

import com.opencsv.bean.AbstractBeanField;
import javafx.beans.property.SimpleStringProperty;

// To parse a CSV file to SimpleProperties
public class StringPropertyConverter extends AbstractBeanField<SimpleStringProperty, String> {

    @Override
    public Object convert(String value) {
       return new SimpleStringProperty(value);
    }

    @Override
    public String convertToWrite(Object value) {
        return (String)value;
    }

}
