package nl.vpro.util;

import java.util.Iterator;

/**
 * An iterator that is also {@link AutoCloseable}.
 * @author Michiel Meeuwissen
 * @since 1.1
 */
public interface CloseableIterator<T> extends Iterator<T>, AutoCloseable {

    static void closeQuietly(AutoCloseable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (Exception e) {
            // ignore
        }
    }


}
