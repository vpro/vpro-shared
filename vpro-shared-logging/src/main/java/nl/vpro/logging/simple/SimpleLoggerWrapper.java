package nl.vpro.logging.simple;

import org.slf4j.event.Level;

/**
 * It is hard to wrap every call to e.g. an {@link org.slf4j.Logger}. For a {@link SimpleLogger} it is easy though.
 * Override {@link SimpleLoggerWrapper#wrapMessage(String)} and wrap an existing logger, and every log entry can be post processed.
 *
 * @author Michiel Meeuwissen
 * @since 1.78
 */
public abstract class SimpleLoggerWrapper implements SimpleLogger<SimpleLoggerWrapper> {
    private final SimpleLogger<?> wrapped;

    public SimpleLoggerWrapper(SimpleLogger<?> wrapped) {
        this.wrapped = wrapped;
    }


    @Override
    public void accept(Level level, String message, Throwable t) {
        wrapped.accept(level, wrapMessage(message), t);
    }

    protected abstract  String wrapMessage(String message);
}
