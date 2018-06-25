package nl.vpro.logging.simple;

import lombok.Getter;

import java.util.function.Function;

import org.slf4j.event.Level;

/**
 * Simply logs everything to a StringBuilder. It also works as a tail (to avoid excessive memory useage if lots is logged)
 * If more than {@link #getMaxLength()} lines are logged, the string will be prefixed by {@link #TRUNK} and the earliest lines are removed.
 * @author Michiel Meeuwissen
 * @since 1.77
 */
public class StringBuilderSimpleLogger extends AbstractStringBuilderSimpleLogger{

    @Getter
    final StringBuilder stringBuilder;


    @lombok.Builder
    private StringBuilderSimpleLogger(
        StringBuilder stringBuilder,
        Level level,
        Long maxLength,
        Function<Level, String> prefix) {
        super(level, maxLength, prefix);
        this.stringBuilder = stringBuilder == null ? new StringBuilder() : stringBuilder;
    }

    public StringBuilderSimpleLogger() {
        this(null, null, null, null);
    }


    @Override
    int getLength() {
        return stringBuilder.length();

    }

    @Override
    void append(CharSequence m) {
        stringBuilder.append(m);
    }

    @Override
    void append(char c) {
        stringBuilder.append(c);
    }


    @Override
    void truncateIfNecessary() {
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
