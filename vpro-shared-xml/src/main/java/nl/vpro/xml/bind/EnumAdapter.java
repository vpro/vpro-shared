package nl.vpro.xml.bind;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * @author Michiel Meeuwissen
 * @since 1.63
 */
public abstract class EnumAdapter<T extends Enum<T>> extends XmlAdapter<String,  T> {

    private final Class<T> enumClass;

    protected EnumAdapter(Class<T> enumClass) {
        this.enumClass = enumClass;
    }


    @Override
    public T unmarshal(String v) throws Exception {
        if (v == null) {
            return null;
        }
        try {
            return Enum.valueOf(enumClass, v.trim());
        } catch (IllegalArgumentException iae) {
            try {
                return Enum.valueOf(enumClass, v.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new JAXBException(e);
            }
        }

    }

    @Override
    public String marshal(T v) throws Exception {
        if (v == null) {
            return null;
        }
        return v.name();

    }
}
