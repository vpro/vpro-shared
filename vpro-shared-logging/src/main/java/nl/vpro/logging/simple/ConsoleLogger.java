package nl.vpro.logging.simple;

import java.io.PrintStream;
import java.time.Instant;

import org.slf4j.event.Level;

/**
 * Simply logs everything to stdout/stderr
 *
 * @author Michiel Meeuwissen
 * @since 2.1
 */
public class ConsoleLogger implements SimpleLogger {

    private Level stderrLevel = Level.WARN;

    @Override
    public void accept(Level level, CharSequence message, Throwable t) {
        PrintStream stream = level.compareTo(stderrLevel) >= 0 ? System.err : System.out;
        stream.println(Instant.now() + "\t" + level.name() + "\t" + message);
    }
}
