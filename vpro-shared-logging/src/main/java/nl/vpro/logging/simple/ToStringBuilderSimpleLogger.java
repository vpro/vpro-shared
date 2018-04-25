package nl.vpro.logging.simple;

import lombok.Getter;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.event.Level;

/**
 * @author Michiel Meeuwissen
 * @since 1.77
 */
public class ToStringBuilderSimpleLogger implements SimpleLogger {

    @Getter
    final StringBuilder stringBuilder;

    public ToStringBuilderSimpleLogger(StringBuilder stringBuilder) {
        this.stringBuilder = stringBuilder;
    }

    public ToStringBuilderSimpleLogger() {
        this(new StringBuilder());
    }

    @Override
    public void accept(Level level, String message, Throwable t) {
        if (stringBuilder.length() > 0) {
            stringBuilder.append('\n');
        }
        stringBuilder.append(level.name()).append(" ").append(message);
        if (t != null) {
            stringBuilder.append('\n');
            stringBuilder.append(ExceptionUtils.getStackTrace(t));
        }
    }
}
