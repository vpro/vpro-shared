package nl.vpro.logging.simple;

import java.time.Clock;
import java.util.function.Consumer;

/**
 * A {@link SimpleLogger} wraps a consumer for every log  {@link Event}'s (or possibly extensions thereof)
 * <p>
 * It is abstract because you need to implement how to instantiate a new {@link Event} extension  via {@link #createEvent(Level, CharSequence, Throwable)}
 * If you have no need for that, you can instantiate via {@link #of(Consumer)}, or use {@link #createEvent(Level, CharSequence, Throwable, Clock)}
 *
 *
 *
 * @author Michiel Meeuwissen
 * @since 1.76
 */
public abstract class EventSimpleLogger<E extends Event> implements SimpleLogger {

    private final Consumer<? super E> consumer;

    protected EventSimpleLogger(Consumer<? super  E> consumer) {
        this.consumer = consumer;
    }

    public static EventSimpleLogger<Event> of(Consumer<Event> consumer) {
        return new EventSimpleLogger<Event>(consumer) {
            @Override
            protected Event createEvent(Level level, CharSequence message, Throwable t) {
                return createEvent(level, message, t, Clock.systemUTC());
            }
        };
    }

    @Override
    public void accept(Level level, CharSequence message, Throwable t) {
        consumer.accept(createEvent(level, message, t));
    }

    @Override
    public void accept(Level level, CharSequence message) {
        consumer.accept(createEvent(level, message));
    }

    protected abstract E createEvent(Level level, CharSequence message, Throwable t);

    protected E createEvent(Level level, CharSequence message) {
        return createEvent(level, message.toString(), null);
    }

    /**
     * An implementation of {@link #createEvent(Level, CharSequence, Throwable)} that just creates {@link Event}s using
     * the given {@link Clock}
     */
    protected Event createEvent(Level level, CharSequence message, Throwable t, Clock clock) {
        return  Event.builder()
            .level(level)
            .message(message)
            .throwable(t)
            .timeStamp(clock.instant())
            .build();
    }

    @Override
    public String toString() {
        return "event logger:" + consumer;
    }

}
