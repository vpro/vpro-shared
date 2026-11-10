package nl.vpro.logging.log4j2;

import java.time.Duration;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.jspecify.annotations.Nullable;

/**
 *
 * @author Michiel Meeuwissen
 * @since 5.14
 */
public class CaptureListFromLogger extends AbstractCaptureLogger implements Supplier<List<LogEvent>> {

    private final List<LogEvent> events = Collections.synchronizedList(new ArrayList<>());

    @lombok.Builder
    private CaptureListFromLogger(
        @Nullable Predicate<LogEvent> predicate,
        @Nullable Level level,
        @Nullable String loggerName,
        @Nullable Class<?> loggerClass,
        @Nullable UUID uuid,
        boolean currentThreadOnly) {
        super(filter(predicate, level, loggerName, loggerClass),  uuid, currentThreadOnly);
    }

    public CaptureListFromLogger(Level level, boolean currentThreadOnly) {
        this(null, level, null, null, null, currentThreadOnly);
    }

    public CaptureListFromLogger(boolean currentThreadOnly) {
        this(Level.INFO, currentThreadOnly);
    }

    public List<LogEvent> getEvents() {
        synchronized (events) {
            return List.copyOf(events);
        }
    }

    @Override
    public List<LogEvent> get() {
        return getEvents();
    }

    @Override
    protected void accept(LogEvent event) {
        synchronized (events) {
            events.add(event.toImmutable());
            events.notifyAll();
        }
    }

    public void clear() {
        events.clear();
    }

    /**
     * Blocks the calling thread until an already captured, or newly arriving, event matches {@code predicate}.
     * <p>
     * Useful when the event is expected to be logged asynchronously (e.g. by another thread), and polling
     * {@link #getEvents()} in a loop would otherwise be the only option.
     *
     * @throws AssertionError if no matching event arrives within {@code timeout}
     * @since 5.20
     */
    public LogEvent awaitFirst(Predicate<LogEvent> predicate, Duration timeout) throws InterruptedException {
        final long deadline = System.nanoTime() + timeout.toNanos();
        synchronized (events) {
            while (true) {
                for (LogEvent event : events) {
                    if (predicate.test(event)) {
                        return event;
                    }
                }
                long remainingMillis = (deadline - System.nanoTime()) / 1_000_000;
                if (remainingMillis <= 0) {
                    throw new AssertionError("No event matching " + predicate + " captured within " + timeout + "; captured: " + events);
                }
                events.wait(remainingMillis);
            }
        }
    }

    /**
     * Shortcut for {@link #awaitFirst(Predicate, Duration)} matching on {@link LogEvent#getLevel()}.
     * @since 5.20
     */
    public LogEvent awaitFirst(Level level, Duration timeout) throws InterruptedException {
        return awaitFirst(event -> event.getLevel().equals(level), timeout);
    }
}
