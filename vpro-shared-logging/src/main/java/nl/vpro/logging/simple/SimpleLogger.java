package nl.vpro.logging.simple;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import static nl.vpro.logging.simple.Level.shiftedLevel;
import static nl.vpro.logging.simple.Slf4jSimpleLogger.slf4j;


/**
 *<p>A very simplified Logger. This can e.g. be used as messaging system. It was made to use in conjuction with {@link ChainedSimpleLogger} to be able to programmaticly 'tee' logging.</p>
 *
 * <p>The goal was to log to slf4j but also send corresponding messages to users via websockets.</p>
 *
 *<p>Generally this can be used when a Logger instance wants to be an argument, because simple loggers can be implemented easily, normally with a just a few lines, and actual loggers can be wrapped easily too.</p>
 *
 * @author Michiel Meeuwissen
 * @since 1.76
 */
@FunctionalInterface
public interface  SimpleLogger extends BiConsumer<Level, CharSequence> {

    ThreadLocal<SimpleLogger> THREAD_LOCAL = ThreadLocal.withInitial(NOPLogger::new);

    static RemoveFromThreadLocal withLogger(SimpleLogger logger) {
        SimpleLogger before = THREAD_LOCAL.get();
        THREAD_LOCAL.set(logger);
        return new RemoveFromThreadLocal(before);
    }

    static SimpleLogger threadLocalOr(SimpleLogger logger) {
        SimpleLogger threadLocal = THREAD_LOCAL.get();
        if (threadLocal instanceof NOPLogger || threadLocal == null) {
            return logger;
        }
        return threadLocal;
    }
    static SimpleLogger threadLocalOr(Logger logger) {
        return threadLocalOr(slf4j(logger));
    }

    class RemoveFromThreadLocal implements  AutoCloseable {

        private final SimpleLogger before;

        public RemoveFromThreadLocal(SimpleLogger before) {
            this.before = before;
        }

        @Override
        public void close()  {
            if (before == null) {
                THREAD_LOCAL.remove();
            } else {
                THREAD_LOCAL.set(before);
            }
        }
    }

    @Deprecated
    static Slf4jSimpleLogger slfj4(Logger log) {
        return new Slf4jSimpleLogger(log);
    }

    static NOPLogger nop() {
        return new NOPLogger();
    }

    static JULSimpleLogger jul(java.util.logging.Logger log) {
        return new JULSimpleLogger(log);
    }


    default String getName() {
        return getClass().getSimpleName();
    }

    default void trace(String format, Object... arg) {
        log(Level.TRACE, format, arg);
    }

    default void debug(String format, Object... arg) {
        log(Level.DEBUG, format, arg);
    }

    default void info(String format, Object... arg) {
        log(Level.INFO, format, arg);
    }

    default void warn(String format, Object... arg) {
        log(Level.WARN, format, arg);
    }

    default void error(String format, Object... arg) {
        log(Level.ERROR, format, arg);
    }

    default void trace(CharSequence message) {
        log(Level.TRACE, message);
    }

    default void trace(Supplier<CharSequence> message) {
        trace(message.get());
    }

    default void debug(CharSequence message) {
        log(Level.DEBUG, message);
    }

    default void debug(Supplier<CharSequence> message) {
        debug(message.get());
    }

    default void info(CharSequence message) {
        log(Level.INFO, message);
    }

    default void info(Supplier<CharSequence> message) {
        info(message.get());
    }

    default void warn(CharSequence message) {
        log(Level.WARN, message);
    }

    default void warn(Supplier<CharSequence> message) {
        warn(message.get());
    }

    default void error(CharSequence message) {
        log(Level.ERROR, message);
    }

    default void error(Supplier<CharSequence> message) {
        error(message.get());
    }

    default void log(Level level, CharSequence message) {
        accept(level, message);
    }

    default void log(Level level, @Nullable CharSequence format, Object... arg) {
        if (isEnabled(level)) {
            if (format == null) {
                format = "null";
            }
            Object[] effectiveArg = new Object[arg.length];
            for (int i = 0; i < arg.length; i++) {
                if (arg[i] instanceof Supplier) {
                    effectiveArg[i] = ((Supplier<?>) arg[i]).get();
                } else {
                    effectiveArg[i] = arg[i];
                }
            }
            final FormattingTuple ft = MessageFormatter.arrayFormat(format.toString(), effectiveArg);
            final String message = ft.getMessage();
            if (ft.getArgArray().length == arg.length) {
                accept(level, message, null);
            } else if (arg.length >= 1){
                final Object t = arg[arg.length - 1];
                if (t instanceof Throwable) {
                    accept(level, message, (Throwable) t);
                } else {
                    accept(level, message, null);
                }
            } else {
                accept(Level.ERROR, "Format has " + ft.getArgArray().length + " args but " + arg.length + " were given", null);
                accept(level, message, null);
            }
        }
    }

    default void debugOrInfo(boolean info, String format, Object... arg) {
        log(info ? Level.INFO : Level.DEBUG, format, arg);
    }

    default boolean isEnabled(Level level) {
        return true;
    }

    default boolean isInfoEnabled() {
        return isEnabled(Level.INFO);
    }

    default boolean isDebugEnabled() {
        return isEnabled(Level.DEBUG);
    }

    @Override
    default void accept(Level level, CharSequence message) {
        accept(level, message, null);
    }

    void accept(Level level, CharSequence message, @Nullable Throwable t);

    /**
     * Returns a new {@link SimpleLogger} that logs to both the current logger and the loggers given as argument.
     * @param logger The loggers to chain
     */
    default SimpleLogger chain(SimpleLogger... logger) {
        SimpleLogger[] array = new SimpleLogger[logger.length + 1];
        array[0] = this;
        System.arraycopy(logger, 0, array, 1, logger.length);
        return new ChainedSimpleLogger(array);
    }


    @Deprecated
    default SimpleLogger chain(Logger... logger) {
        return Slf4jSimpleLogger.chain(this, logger);
    }

    /**
     * Returns a new {@link SimpleLogger} which will never log higher then {@code maxLevel}.
     * @since 3.1
     */
    default SimpleLogger truncated(Level maxLevel) {
        SimpleLogger wrapped = this;
        return new SimpleLogger() {
            @Override
            public void accept(Level level, CharSequence message, @Nullable Throwable t) {
                if (level.compareTo(maxLevel) < 0) {
                    level = maxLevel;
                }
                wrapped.accept(level, message, t);
            }
            @Override
            public boolean isEnabled(Level level) {
                return wrapped.isEnabled(level);
            }
        };
    }
    /**
     * Returns a new {@link SimpleLogger} with shifted levels.
     * @since 3.1
     * @param shift The amount to shift. Positive values will shift to {@link Level#TRACE}, negative values to {@link Level#ERROR}
     */
    default SimpleLogger shift(int shift) {
        SimpleLogger wrapped = this;
        return new SimpleLogger() {
            @Override
            public void accept(Level level, CharSequence message, @Nullable Throwable t) {
                wrapped.accept(shiftedLevel(level, shift), message, t);
            }
            @Override
            public boolean isEnabled(Level level) {
                return wrapped.isEnabled(shiftedLevel(level, shift));
            }
        };
    }
}
