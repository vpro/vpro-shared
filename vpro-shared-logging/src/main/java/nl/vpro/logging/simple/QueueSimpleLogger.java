package nl.vpro.logging.simple;

import java.time.Clock;
import java.time.Instant;
import java.util.Queue;

/**
 * A {@link SimpleLogger} that adds every log event to a {@link Queue} of {@link Event}'s (or possibly extensions thereof)
 *
 * It is abstract because you need to implement how to instantiate a new {@link Event} extension for the queue via {@link #createEvent(Level, CharSequence, Throwable)}
 * If you have no need for that, you can instantiate via {@link #of(Queue)}.
 *
 * @author Michiel Meeuwissen
 * @since 1.76
 */
public abstract class QueueSimpleLogger<E extends Event> extends EventSimpleLogger<E> {

    private final Queue<E> queue;

    protected QueueSimpleLogger(Queue<E> queue) {
        super(queue::add);
        this.queue = queue;
    }

    public static QueueSimpleLogger<nl.vpro.logging.simple.Event> of(Queue<nl.vpro.logging.simple.Event> q, Clock clock) {
        return new QueueSimpleLogger<nl.vpro.logging.simple.Event>(q) {
            @Override
            protected nl.vpro.logging.simple.Event createEvent(Level level, CharSequence message, Throwable t) {
                return createEvent(level, message, t, clock);
            }
        };
    }

    /**
     * Creates a straight forward instance for a {@link Queue}
     */
    public static QueueSimpleLogger<nl.vpro.logging.simple.Event> of(Queue<nl.vpro.logging.simple.Event> q) {
        return of(q, Clock.systemUTC());
    }


    @Override
    public String toString() {
        return "queue:" + queue;
    }

    /**
     * @deprecated Just use {@link nl.vpro.logging.simple.Event}
     */
    @Deprecated
    public static class Event  extends nl.vpro.logging.simple.Event {
        protected Event(Level level, CharSequence message, Throwable throwable, Instant timeStamp) {
            super(level, message, throwable, timeStamp);
        }
    }
}
