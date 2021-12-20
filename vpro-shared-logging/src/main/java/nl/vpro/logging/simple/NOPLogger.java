package nl.vpro.logging.simple;

/**
 * @author Michiel Meeuwissen
 * @since 1.77
 */
public class NOPLogger implements SimpleLogger {
    @Override
    public void accept(Level level, CharSequence message, Throwable t) {

    }

    @Override
    public boolean isEnabled(Level level) {
        return false;
    }


    @Override
    public String toString() {
        return "NOP";
    }
}
