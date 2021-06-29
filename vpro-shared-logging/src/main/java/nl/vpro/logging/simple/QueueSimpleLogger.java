package nl.vpro.logging.simple;

import lombok.Getter;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Queue;

import org.slf4j.MDC;
import org.slf4j.event.Level;

/**
 * A {@link SimpleLogger} that adds every log event to a {@link Queue} of {@link Event}'s (or possibly extensions thereof)
 *
 * It is abstract because you need to implement how to instantiate a new {@link Event} extension for the queue via {@link #createEvent(Level, CharSequence, Throwable)}
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


    public static QueueSimpleLogger<Event> of(Queue<Event> q, Clock clock) {
        return new QueueSimpleLogger<Event>(q) {
            @Override
            protected Event createEvent(Level level, CharSequence message, Throwable t) {
                return  Event.builder()
                    .level(level)
                    .message(message)
                    .throwable(t)
                    .timeStamp(clock.instant())
                    .build();
            }
        };
    }

    /**
     * Creates a straight forward instance for a {@link Queue}
     */
    public static QueueSimpleLogger<Event> of(Queue<Event> q) {
        return of(q, Clock.systemUTC());
    }

    @Override
    public void accept(Level level, CharSequence message, Throwable t) {
        queue.add(createEvent(level, message, t));
    }

    @Override
    public void accept(Level level, CharSequence message) {
        queue.add(createEvent(level, message));
    }


    protected abstract E createEvent(Level level, CharSequence message, Throwable t);

    protected E createEvent(Level level, CharSequence message) {
        return createEvent(level, message.toString(), null);
    }


    @Override
    public String toString() {
        return "queue:" + queue;
    }


    /**
     * A representation of a log event
     */
    @Getter
    public static class Event {
        private final Instant timeStamp;
        private final Level level;
        private final CharSequence message;
        private final Throwable throwable;
        private final Map<String, String> mdc = MDC.getCopyOfContextMap();
        private final int levelInt;

        @lombok.Builder
        protected Event(Level level, CharSequence message, Throwable throwable, Instant timeStamp) {
            this.level = level;
            this.message = message;
            this.throwable = throwable;
            this.levelInt = level.toInt();
            this.timeStamp = timeStamp == null ? Instant.now() : timeStamp;
        }

        @Override
        public String toString() {
            return timeStamp + ":" + level + ":" + message;
        }
    }
}
