package nl.vpro.logging.simple;

/**
 * It is hard to wrap every call to e.g. an {@link org.slf4j.Logger}. For a {@link SimpleLogger} it is easy though.
 * Override {@link SimpleLoggerWrapper#wrapMessage(CharSequence)} and wrap an existing logger, and every log entry can be post processed.
 *
 * @author Michiel Meeuwissen
 * @since 1.78
 */
public abstract class SimpleLoggerWrapper implements SimpleLogger {
    private final SimpleLogger wrapped;

    public SimpleLoggerWrapper(SimpleLogger wrapped) {
        this.wrapped = wrapped;
    }


    @Override
    public void accept(Level level, CharSequence message, Throwable t) {
        wrapped.accept(level, wrapMessage(level, message), t);
    }

    @Override
    public boolean isEnabled(Level level) {
        return wrapped.isEnabled(level);
    }
    @Override
    public String getName() {
        return wrapped.getName();
    }

    protected String wrapMessage(CharSequence message) {
        return message.toString();
    }

    protected String wrapMessage(Level level, CharSequence message) {
        return wrapMessage(message);
    }

}
