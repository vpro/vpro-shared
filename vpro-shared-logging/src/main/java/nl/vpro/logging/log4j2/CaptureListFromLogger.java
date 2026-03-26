package nl.vpro.logging.log4j2;

import lombok.Getter;

import java.util.*;
import java.util.function.Supplier;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;

/**
 *
 * @author Michiel Meeuwissen
 * @since 5.14
 */
public class CaptureListFromLogger extends AbstractCaptureLogger implements Supplier<List<LogEvent>> {

    @Getter
    private final List<LogEvent> events = new ArrayList<>();



    private CaptureListFromLogger(Level level, UUID uuid, boolean currentThreadOnly) {
        super(level, uuid, currentThreadOnly);
    }

    public CaptureListFromLogger(Level level, boolean currentThreadOnly) {
        this(level, UUID.randomUUID(), currentThreadOnly);
    }

    public CaptureListFromLogger(boolean currentThreadOnly) {
        this(Level.INFO, currentThreadOnly);
    }



    @Override
    public List<LogEvent> get() {
        return Collections.unmodifiableList(getEvents());
    }

    @Override
    protected void accept(LogEvent event) {
        events.add(event.toImmutable());
    }

    public void clear() {
        events.clear();
    }
}
