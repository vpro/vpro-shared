package nl.vpro.util;

import java.util.Iterator;
import java.util.Optional;

/**
 * @author Michiel Meeuwissen
 * @since 0.31
 */
public interface CountedIterator<T> extends Iterator<T> {
    Optional<Long> getSize();

    /**
     * If the iterator is in some way restricted you may also want to report a total size, representing the unrestricted size.
     */
    default Optional<Long> getTotalSize() {
        return getSize();
    }
}

