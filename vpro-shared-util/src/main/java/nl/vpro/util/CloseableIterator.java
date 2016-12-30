package nl.vpro.util;

import java.util.Iterator;

/**
 * @author Michiel Meeuwissen
 * @since 1.1
 */
public interface CloseableIterator<T> extends Iterator<T>, AutoCloseable {

}
