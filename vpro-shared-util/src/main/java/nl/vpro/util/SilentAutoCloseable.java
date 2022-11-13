package nl.vpro.util;

/**
 * Like {@link AutoCloseable}, but {@link #close()} doesn't throw.
 */
public interface SilentAutoCloseable extends AutoCloseable {

    @Override
    void close();
}
