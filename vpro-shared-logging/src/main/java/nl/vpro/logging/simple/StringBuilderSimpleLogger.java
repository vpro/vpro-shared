package nl.vpro.logging.simple;

import lombok.Getter;

import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.event.Level;

/**
 * Simply logs everything to a StringBuilder. It also works as a tail (to avoid excessive memory useage if lots is logged)
 * If more than {@link #getMaxLength()} lines are logged, the string will be prefixed by {@link #TRUNK} and the earliest lines are removed.
 * @author Michiel Meeuwissen
 * @since 1.77
 */
public class StringBuilderSimpleLogger implements SimpleLogger {

    private static final String TRUNK = "...\n";

    @Getter
    final StringBuilder stringBuilder;

    @Getter
    final long maxLength;
    private long count = 0;
    private boolean truncated = false;

    private Level level = Level.INFO;

    final private Function<Level, String> prefix;

    @lombok.Builder
    private StringBuilderSimpleLogger(
        StringBuilder stringBuilder,
        Level level,
        Long maxLength,
        Function<Level, String> prefix) {
        this.stringBuilder = stringBuilder == null ? new StringBuilder() : stringBuilder;
        this.maxLength = maxLength == null ? 10000L : maxLength;
        this.level = level == null ? Level.INFO : level;
        this.prefix = prefix == null ? Enum::name : prefix;
    }

    public StringBuilderSimpleLogger() {
        this(null, null, null, null);
    }

    @Override
    public void accept(Level level, CharSequence message, Throwable t) {
        if (level.toInt() < this.level.toInt()) {
            return;
        }
        if (stringBuilder.length() > 0) {
            stringBuilder.append('\n');
            count++;
        }
        String p = prefix.apply(level);
        stringBuilder.append(p);
        if (p.length() > 0) {
            stringBuilder.append(" ");
        }
        stringBuilder.append(message);
        if (t != null) {
            stringBuilder.append('\n');
            count++;
            String stackTrace = ExceptionUtils.getStackTrace(t);
            count += StringUtils.countMatches(stackTrace, '\n');
            stringBuilder.append(stackTrace);
        }
        truncateIfNecessary();
    }

    @Override
    public String toString() {
        return "string buffer with " + count + " lines";
    }



    private void truncateIfNecessary() {
        while (count >= maxLength) {
            if (! truncated) {
                stringBuilder.insert(0, TRUNK);
                truncated = true;
            }
            int index = stringBuilder.indexOf("\n", TRUNK.length());
            stringBuilder.delete(TRUNK.length(),  index + 1);
            count--;
        }
    }
}
