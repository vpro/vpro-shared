package nl.vpro.xml.bind;

import java.time.Duration;
import java.time.format.DateTimeParseException;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import org.meeuw.time.TimeUtils;


/**
 * This simpler version of {@link DurationXmlAdapter} simply uses {@link Duration#toString()}
 * https://bugs.openjdk.java.net/browse/JDK-8042456
 * @author Michiel Meeuwissen
 * @since 0.36
 */
public class DefaultDurationXmlAdapter extends XmlAdapter<String, Duration> {

    @Override
    public Duration unmarshal(String stringValue) {
        try {
            return stringValue != null ? Duration.parse(stringValue) : null;
        } catch (DateTimeParseException dtpe) {
            return Duration.ofMillis(TimeUtils.parse(stringValue)
                .orElseThrow(() -> new IllegalArgumentException(stringValue + " cannot be parse to duration")).toEpochMilli());
        }
    }

    @Override
    public String marshal(Duration value) {
        return value != null ? value.toString() : null;
    }
}
