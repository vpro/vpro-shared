package nl.vpro.logging.simple;

import org.slf4j.event.Level;

/**
 * @author Michiel Meeuwissen
 * @since 1.77
 */
public class NOPLogger implements SimpleLogger<NOPLogger> {
    @Override
    public void accept(Level level, String message, Throwable t) {

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
