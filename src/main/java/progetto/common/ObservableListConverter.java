package progetto.common;

import com.opencsv.bean.AbstractBeanField;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// To parse a CSV field to ObservableList
public class ObservableListConverter extends AbstractBeanField {

    // The recipients' addresses can't contain commas (,)
    @Override
    public Object convert(String value) {
        System.out.println("Try de-converting...");
        return FXCollections.observableArrayList(new ArrayList<>(Arrays.asList(value.split(","))));
    }

    @Override
    public String convertToWrite(Object value) {
        System.out.println("Try converting...");
        return String.join(", ", (List)value);
    }

}
