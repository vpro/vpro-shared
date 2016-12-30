package nl.vpro.xml.bind;

import java.time.Duration;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * This simpler version of {@link DurationXmlAdapter} simply uses {@link Duration#toString()}
 * https://bugs.openjdk.java.net/browse/JDK-8042456
 * @author Michiel Meeuwissen
 * @since 0.36
 */
public class DefaultDurationXmlAdapter extends XmlAdapter<String, Duration> {

    @Override
    public Duration unmarshal(String stringValue) {
        return stringValue != null ? Duration.parse(stringValue) : null;
    }

    @Override
    public String marshal(Duration value) {
        return value != null ? value.toString() : null;
    }
}
