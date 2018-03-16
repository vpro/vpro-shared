package nl.vpro.logging;

import lombok.AccessLevel;
import lombok.Getter;

import java.util.Map;
import java.util.Queue;

import org.slf4j.MDC;
import org.slf4j.event.Level;

/**
 * @author Michiel Meeuwissen
 * @since 1.76
 */
public abstract class QueueSimpleLogger<E extends QueueSimpleLogger.Event> implements SimpleLogger {

    private final Queue<E> queue;

    public QueueSimpleLogger(Queue<E> queue) {
        this.queue = queue;
    }

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
