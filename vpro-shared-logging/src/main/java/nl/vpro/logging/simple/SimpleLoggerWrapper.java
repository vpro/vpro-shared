package nl.vpro.logging.simple;

import org.slf4j.event.Level;

/**
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
