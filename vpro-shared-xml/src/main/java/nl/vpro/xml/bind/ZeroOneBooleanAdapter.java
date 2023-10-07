package nl.vpro.xml.bind;

import jakarta.xml.bind.DatatypeConverter;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Serializes <tt>boolean</tt> as 0 or 1.
 *
 * @author Michiel Meeuwissen
 * @since 2.24
 */
public class ZeroOneBooleanAdapter extends XmlAdapter<String,Boolean> {

    public Boolean unmarshal(String v) {
        if(v == null) {
            return null;
        }
        return DatatypeConverter.parseBoolean(v);
    }

    public String marshal(Boolean v) {
        if (v == null) {
            return null;
        }
        return v ? "1" : "0";
    }
}
