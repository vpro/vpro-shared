package nl.vpro.logging.simple;

import lombok.Getter;

import java.util.function.Function;


/**
 * Simply logs everything to a StringBuilder. It also works as a tail (to avoid excessive memory usage if lots is logged)
 * If more than {@link AbstractStringBuilderSimpleLogger#getMaxLength()} lines are logged, the string will be prefixed by {@link #TRUNK} and the earliest lines are removed.
 * @author Michiel Meeuwissen
 * @since 1.77
 */
@Getter
public class StringBuilderSimpleLogger extends AbstractStringBuilderSimpleLogger implements StringSupplierSimpleLogger {

    final StringBuilder stringBuilder;


    @lombok.Builder(builderClassName = "Builder")
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

    /**
     * Convenience method to get the length of the underlying {@link #getStringBuilder() StringBuilder}
     * @since 5.5
     */
    @Override
    public int getLength() {
        return stringBuilder.length();
    }

    /**
     * Convenience method to set the length of the underlying {@link #getStringBuilder() StringBuilder}
     * @since 5.5
     */
    public void setLength(int newLength) {
        stringBuilder.setLength(newLength);
    }

    /**
     * Convenience method to empty the underlying {@link #getStringBuilder() StringBuilder}
     * @since 5.5
     */
    public void empty() {
        setLength(0);
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

    @Override
    public String get() {
        return stringBuilder.toString();
    }

    @Override
    public StringSupplierSimpleLogger chain(SimpleLogger... logger) {
        SimpleLogger[] array = new SimpleLogger[logger.length + 1];
        array[0] = this;
        System.arraycopy(logger, 0, array, 1, logger.length);
        return new StringSupplierChainedSimpleLogger(array);
    }

    public static class Builder {

        public StringSupplierSimpleLogger chain(SimpleLogger... logger) {
            return build().chain(logger);
        }
    }

}
