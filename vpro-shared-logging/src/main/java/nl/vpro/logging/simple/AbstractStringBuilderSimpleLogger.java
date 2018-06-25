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
 * @since 1.79
 */
public abstract class AbstractStringBuilderSimpleLogger implements SimpleLogger {

    static final String TRUNK = "...\n";

    @Getter
    final long maxLength;
    @Getter
    protected long count = 0;
    @Getter
    boolean truncated = false;

    private Level level = Level.INFO;

    final private Function<Level, String> prefix;

    protected  AbstractStringBuilderSimpleLogger(
        Level level,
        Long maxLength,
        Function<Level, String> prefix) {
        this.maxLength = maxLength == null ? 10000L : maxLength;
        this.level = level == null ? Level.INFO : level;
        this.prefix = prefix == null ? Enum::name : prefix;
    }

    protected AbstractStringBuilderSimpleLogger() {
        this(null, null, null);
    }

    @Override
    public void accept(Level level, CharSequence message, Throwable t) {
        if (level.toInt() < this.level.toInt()) {
            return;
        }
        if (getLength() > 0) {
            append('\n');
            count++;
        }
        String p = prefix.apply(level);
        append(p);
        if (p.length() > 0) {
            append(" ");
        }
        append(message);
        if (t != null) {
            append('\n');
            count++;
            String stackTrace = ExceptionUtils.getStackTrace(t);
            count += StringUtils.countMatches(stackTrace, '\n');
            append(stackTrace);
        }
        truncateIfNecessary();
    }

    abstract  int getLength();

    abstract void append(CharSequence m);

    abstract void append(char c);


    @Override
    public String toString() {
        return "string buffer with " + count + " lines";
    }



    abstract void truncateIfNecessary();
}
