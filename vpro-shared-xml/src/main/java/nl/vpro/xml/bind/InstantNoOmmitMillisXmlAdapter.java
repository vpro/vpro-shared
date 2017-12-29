package nl.vpro.xml.bind;

import java.time.Instant;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import nl.vpro.util.TimeUtils;

import static nl.vpro.xml.bind.InstantXmlAdapter.formatWithMillis;

/**
 * Like {@link InstantXmlAdapter}, but never omit millis. So no need for the thread local
 * @since 1.75
 */
public class InstantNoOmmitMillisXmlAdapter extends XmlAdapter<String, Instant> {


    @Override
    public Instant unmarshal(String dateValue) {
        return TimeUtils.parse(dateValue).orElse(null);
    }

    @Override
    public String marshal(Instant value) {
       return formatWithMillis(value);
    }

}
