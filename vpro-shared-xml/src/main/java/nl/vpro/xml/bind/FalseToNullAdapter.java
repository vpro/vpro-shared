package nl.vpro.xml.bind;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * @author Michiel Meeuwissen
 * @since 1.7
 */
public class FalseToNullAdapter extends XmlAdapter<Boolean, Boolean> {

    @Override
    public Boolean unmarshal(Boolean bool) {
        return bool == null ? false : bool;
    }

    @Override
    public Boolean marshal(Boolean  bool) {
        return bool == null || !bool ? null : true;
    }
}
