package nl.vpro.hibernate.search;

import java.util.Objects;

/**
 * @author Michiel Meeuwissen
 * @since 2.1
 */
public class IterableCharSequenceBridge extends IterableToStringBridge<CharSequence> {

    @Override
    protected String toString(CharSequence object) {
        return Objects.toString(object);
    }
}
