package nl.vpro.logging.simple;

import lombok.Getter;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.event.Level;

/**
 * @author Michiel Meeuwissen
 * @since 1.77
 */
public class ToStringBuilderSimpleLogger implements SimpleLogger {

    private static final String TRUNC = "...\n";

    @Getter
    final StringBuilder stringBuilder;

    @Getter
    final long maxLength;
    private long count = 0;
    private boolean truncated = false;

    @lombok.Builder
    private ToStringBuilderSimpleLogger(
        StringBuilder stringBuilder,
        Long maxLength) {
        this.stringBuilder = stringBuilder == null ? new StringBuilder() : stringBuilder;
        this.maxLength = maxLength == null ? 10000L : maxLength;
    }

    public ToStringBuilderSimpleLogger() {
        this(null, null);
    }

    @Override
    public void accept(Level level, String message, Throwable t) {

        if (stringBuilder.length() > 0) {
            stringBuilder.append('\n');
            count++;
        }
        stringBuilder.append(level.name()).append(" ").append(message);
        if (t != null) {
            stringBuilder.append('\n');
            count++;
            String stackTrace = ExceptionUtils.getStackTrace(t);
            count += StringUtils.countMatches(stackTrace, '\n');
            stringBuilder.append(stackTrace);
        }
        trucatedIfNecessary();
    }

    private void trucatedIfNecessary() {
        while (count >= maxLength) {
            if (! truncated) {
                stringBuilder.insert(0, TRUNC);
                truncated = true;
            }
            int index = stringBuilder.indexOf("\n", TRUNC.length());
            stringBuilder.delete(TRUNC.length(),  index + 1);
            count--;
        }
    }
}
