package progetto.common.converters;

import com.opencsv.bean.AbstractBeanField;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Converts an Object property containing a Local Date to String, required to parse this field to CSV
 */
public class LocalDateConverter extends AbstractBeanField<ObjectProperty<LocalDate>, String> {
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");          // Date format

    @Override
    protected Object convert(String s) {
        return new SimpleObjectProperty<>(LocalDate.parse(s, formatter));   // Create a Local Date from string
    }

    @Override
    public String convertToWrite(Object value) {
        return ((LocalDate)value).format(formatter);                        // Create a new formatted string
    }

}
