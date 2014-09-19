package nl.vpro.util;

import java.io.Closeable;
import java.util.Iterator;

/**
 * @author Michiel Meeuwissen
 * @since 1.1
 */
public interface CloseableIterator<T> extends Iterator<T>, Closeable {

}
