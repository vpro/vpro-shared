package nl.vpro.xml.bind;

import java.time.Instant;
import java.util.Date;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * https://bugs.openjdk.java.net/browse/JDK-8042456
 * @author Michiel Meeuwissen
 * @since 0.21
 */
public class InstantXmlAdapter extends XmlAdapter<Date, Instant> {

    @Override
    public Instant unmarshal(Date dateValue) {
        return dateValue != null ? Instant.ofEpochMilli(dateValue.getTime()) : null;
    }

    @Override
    public Date marshal(Instant value) {
        return value != null ? new Date(value.toEpochMilli()) : null;
    }
}
