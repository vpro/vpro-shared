package nl.vpro.xml.bind;

import java.util.Locale;

import javax.xml.bind.annotation.adapters.XmlAdapter;


/**
 * Adaption for xml:lang.
 * @author Michiel Meeuwissen
 * @since 0.47
 */
public class LocaleAdapter extends XmlAdapter<String, Locale> {

    @Override
    public Locale unmarshal(String locale) throws Exception {
        String [] parts = locale.split("[\\-_]", 3);
        if (parts.length == 1) {
            return new Locale(parts[0]);
        } else if (parts.length == 2) {
            return new Locale(parts[0], parts[1]);
        } else if (parts.length == 3) {
            return new Locale(parts[0], parts[1], parts[2]);
        } else {
            throw  new IllegalStateException();
        }
    }

    @Override
    public String marshal(Locale locale) throws Exception {
        if (locale == null) {
            return null;
        }
        String toString = locale.toString();
        CharSequence[] parts = toString.split("_", 3);
        return String.join("-", parts);
    }
}
