package nl.vpro.hibernate.search6;

import java.util.Objects;

/**
 * @author Michiel Meeuwissen
 * @since 2.2
 */
public class IterableCharSequenceBridge extends IterableToStringBridge<CharSequence> {


    @Override
    protected String toString(CharSequence object) {
        return Objects.toString(object);
    }
}
