package nl.vpro.logging.simple;

import lombok.Getter;

import java.time.Instant;
import java.util.Map;

import org.apache.logging.log4j.ThreadContext;
import org.slf4j.MDC;


/**
 * A representation of a log event
 */
@Getter
public class Event {
    private final Instant timeStamp;
    private final Level level;
    private final CharSequence message;
    private final Throwable throwable;
    private final Map<String, String> mdc;
    private final int levelInt;

    @lombok.Builder
    protected Event(Level level, CharSequence message, Throwable throwable, Instant timeStamp, Map<String, String> mdc) {
        this.level = level;
        this.message = message;
        this.throwable = throwable;
        this.levelInt = level.toInt();
        this.timeStamp = timeStamp;
        this.mdc = mdc == null ? createMdc() : mdc;
    }

    static Map<String, String> createMdc() {
        try {
            Map<String, String> mdc = MDC.getCopyOfContextMap();
            return mdc == null ? Map.of() : mdc;
        } catch (Throwable e) {
            try {
                return ThreadContext.getImmutableContext();
            } catch (Throwable e2) {
                return Map.of();
            }
        }
    }

    @Override
    public String toString() {
        return (timeStamp == null ? "" : (timeStamp + ":")) + level + ":" + message + (throwable == null ? "" : ":" + throwable.getClass().getName() + ":" + throwable.getMessage());
    }
}
