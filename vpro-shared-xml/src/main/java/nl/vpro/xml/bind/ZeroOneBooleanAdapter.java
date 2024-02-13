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
        try {
            return DatatypeConverter.parseBoolean(v);
        } catch (IllegalArgumentException iae) {
            //In jaxb.DataTypeConverter this was the behaviour.
            // I suppose the change was good, but for now keep it compatible
            return false;
        }
    }

    public String marshal(Boolean v) {
        if (v == null) {
            return null;
        }
        return v ? "1" : "0";
    }
}
