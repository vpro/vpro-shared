package nl.vpro.xml.bind;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

/**
 * https://bugs.openjdk.java.net/browse/JDK-8042456
 * @author Michiel Meeuwissen
 * @since 0.21
 */
public class DurationXmlAdapter extends XmlAdapter<javax.xml.datatype.Duration, Duration> {


    private static DatatypeFactory datatypeFactory;
    static {
        try {
            datatypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException();
        }
    }
    @Override
    public Duration unmarshal(javax.xml.datatype.Duration stringValue) {
        return stringValue != null ? Duration.parse(stringValue.toString()) : null;
    }

    @Override
    public javax.xml.datatype.Duration marshal(Duration value) {
        return value != null ? (value.toDays() < 30 ? marshalDayTime(value.toMillis()) : marshal(value.toMillis())) : null;
    }

    protected javax.xml.datatype.Duration marshalDayTime(long time)  {
        return datatypeFactory.newDurationDayTime(time);
    }
    
    protected javax.xml.datatype.Duration marshal(long time)  {
        return datatypeFactory.newDuration(time);
    }
}
