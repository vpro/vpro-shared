package nl.vpro.logging.simple;

import java.time.Clock;
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

    public static QueueSimpleLogger<Event> of(Queue<Event> q, Clock clock) {
        return new QueueSimpleLogger<Event>(q) {
            @Override
            protected Event createEvent(Level level, CharSequence message, Throwable t) {
                return createEvent(level, message, t, clock);
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
    public String toString() {
        return "queue:" + queue;
    }

}
