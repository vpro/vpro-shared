package nl.vpro.logging.simple;

/**
 * SimpleLogger that wraps Java Platform Logger (@since java 9)
 * @author Michiel Meeuwissen
 * @since 5.13
 */
public class JPLSimpleLogger implements SimpleLogger {
    private final System.Logger log;

    public JPLSimpleLogger(System.Logger log) {
        this.log = log;
    }

    @Override
    public void accept(Level level, CharSequence message, Throwable t) {
        log.log(toLevel(level), message.toString(), t);
    }


    @Override
    public boolean isEnabled(Level level) {
        return log.isLoggable(toLevel(level));
    }

    public static System.Logger.Level toLevel(Level level) {
        return switch (level) {
            case TRACE -> System.Logger.Level.TRACE;
            case DEBUG -> System.Logger.Level.DEBUG;
            case INFO -> System.Logger.Level.INFO;
            case WARN -> System.Logger.Level.WARNING;
            default -> System.Logger.Level.ERROR;
        };
    }

}
