package progetto.common.converters;

import com.opencsv.bean.AbstractBeanField;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class LocalDateConverter extends AbstractBeanField<ObjectProperty<LocalDate>, String> {
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    protected Object convert(String s) {
        return new SimpleObjectProperty<>(LocalDate.parse(s, formatter));
    }

    @Override
    public String convertToWrite(Object value) {
        return ((LocalDate)value).format(formatter);
    }

}
