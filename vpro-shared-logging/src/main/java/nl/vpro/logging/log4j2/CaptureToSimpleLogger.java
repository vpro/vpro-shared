package nl.vpro.logging.log4j2;

import org.apache.logging.log4j.core.LogEvent;

import nl.vpro.logging.simple.Level;
import nl.vpro.logging.simple.SimpleLogger;

/**
 * Simple setup using log4j2 to temporarily capture logging and collect it to a SimpleLogger.
 * <p>
 * usage
 * <pre>{@code
 *
 *   try (CaptureToSimpleLogger capture = CaptureToSimpleLogger.of(simpleLogger)) {
 *
 *       stuff, which might log some things (as long as in same thread!)
 *
 *       publication.setMessages(capture.get())
 *   }
 * }
 * </pre>
 *
 *
 * @author Michiel Meeuwissen
 * @since 5.11
 */
public class CaptureToSimpleLogger extends AbstractCaptureLogger {

    public static CaptureToSimpleLogger of(SimpleLogger simpleLogger) {
        return new CaptureToSimpleLogger(simpleLogger);
    }

    private final SimpleLogger simpleLogger;
    private CaptureToSimpleLogger(SimpleLogger simpleLogger) {
        this.simpleLogger = simpleLogger;
    }

    @Override
    protected void accept(LogEvent event) {
        String m = event.getMessage().getFormattedMessage();
        simpleLogger.accept(
            Level.valueOf(event.getLevel().name()),
            m,
            event.getThrown()
        );
    }
}
