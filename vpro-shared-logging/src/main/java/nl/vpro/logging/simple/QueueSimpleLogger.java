package nl.vpro.logging.simple;

import lombok.AccessLevel;
import lombok.Getter;

import java.util.Map;
import java.util.Queue;

import org.slf4j.MDC;
import org.slf4j.event.Level;

/**
 * A {@link SimpleLogger} that adds every log event to a {@link Queue} of {@link Event}'s (or possibly extensions thereof)
 *
 * It is abstract because you need to implement how to instantiate a new {@link Event} extension for the queue via {@link #createEvent(Level, String, Throwable)}
 * If you have no need for that, you can instantiate via {@link #of(Queue)}.
 *
 * @author Michiel Meeuwissen
 * @since 1.76
 */
public abstract class QueueSimpleLogger<E extends QueueSimpleLogger.Event> implements SimpleLogger {

    private final Queue<E> queue;

    protected QueueSimpleLogger(Queue<E> queue) {
        this.queue = queue;
    }

    /**
     * Creates a straighforward instance for a {@link Queue<Event>}
     */
    public static QueueSimpleLogger<Event> of(Queue<Event> q) {
        return new QueueSimpleLogger<Event>(q) {
            @Override
            protected Event createEvent(Level level, String message, Throwable t) {
                return  Event.builder()
                    .level(level)
                    .message(message)
                    .throwable(t)
                    .build();
            }
        };
    }

    @Override
    public void accept(Level level, String message, Throwable t) {
        queue.add(createEvent(level, message, t));
    }

    protected abstract E createEvent(Level level, String message, Throwable t);


    /**
     * A representation of a log event
     */
    @Getter
    @lombok.AllArgsConstructor(access = AccessLevel.PROTECTED)
    @lombok.Builder
    public static class Event {
        private final Level level;
        private final String message;
        private final Throwable throwable;
        private final Map<String, String> mdc = MDC.getCopyOfContextMap();
    }
}
