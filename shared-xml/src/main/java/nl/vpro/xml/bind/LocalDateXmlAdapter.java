package nl.vpro.xml.bind;

import java.time.LocalDate;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * https://bugs.openjdk.java.net/browse/JDK-8042456
 * @author Michiel Meeuwissen
 * @since 0.28
 */
public class LocalDateXmlAdapter extends XmlAdapter<String, LocalDate> {



    @Override
    public LocalDate unmarshal(String dateValue) {
        if (dateValue == null) {
            return null;
        }
        return LocalDate.parse(dateValue);
    }

    @Override
    public String marshal(LocalDate value) {
        return value != null ? value.toString() : null;
    }
}
