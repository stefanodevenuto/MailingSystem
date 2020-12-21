package progetto.common.converters;

import com.opencsv.bean.AbstractBeanField;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Converts an Observable List to a list of strings, required to parse this field to CSV
 */
public class ObservableListConverter extends AbstractBeanField<ObservableList<String>, String> {

    // The recipients' addresses can't contain commas (,)
    @Override
    public Object convert(String value) {
        return FXCollections.observableArrayList(new ArrayList<>(Arrays.asList(value.split(","))));
    }

    @Override
    public String convertToWrite(Object value) {
        return String.join(",", (List)value);
    }

}
