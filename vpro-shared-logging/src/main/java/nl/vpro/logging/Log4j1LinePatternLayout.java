package nl.vpro.logging;

import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

@Deprecated
public class Log4j1LinePatternLayout extends PatternLayout {

    @Override
    public boolean ignoresThrowable() {
        return false;
    }

    @Override
    public String format(LoggingEvent event) {
        final ThrowableInformation throwableInformation = event.getThrowableInformation();

        if (throwableInformation != null) {
            StringBuilder sb = new StringBuilder(event.getRenderedMessage());
            sb.append("\n");

            if (throwableInformation.getThrowable() != null) {
                sb.append(throwableInformation.getThrowable().getMessage()).append(": \n");
            }

            for (String line : throwableInformation.getThrowableStrRep()) {
                sb.append(line).append('\n');
            }

            event = new LoggingEvent(
                event.getFQNOfLoggerClass(),
                event.getLogger(),
                event.getTimeStamp(),
                event.getLevel(),
                sb.toString(),
                event.getThreadName(),
                throwableInformation,
                event.getNDC(),
                event.getLocationInformation(),
                event.getProperties()
            );
        }

        return super.format(event).replaceAll("\n", " | ") + "\n";
    }
}
