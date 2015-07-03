package nl.vpro.xml.bind;

import java.time.Duration;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * https://bugs.openjdk.java.net/browse/JDK-8042456
 * @author Michiel Meeuwissen
 * @since 0.21
 */
public class DurationXmlAdapter extends XmlAdapter<String, Duration> {

    @Override
    public Duration unmarshal(String stringValue) {
        return stringValue != null ? Duration.parse(stringValue) : null;
    }

    @Override
    public String marshal(Duration value) {
        return value != null ? value.toString() : null;
    }
}
