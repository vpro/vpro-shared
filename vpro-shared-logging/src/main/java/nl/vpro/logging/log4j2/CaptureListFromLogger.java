package nl.vpro.logging.log4j2;

import lombok.Getter;

import java.util.*;
import java.util.function.Supplier;

import org.apache.logging.log4j.core.LogEvent;

/**
 *
 * @author Michiel Meeuwissen
 * @since 5.14
 */
public class CaptureListFromLogger extends AbstractCaptureLogger implements Supplier<List<LogEvent>> {

    @Getter
    private final List<LogEvent> events = new ArrayList<>();

    private CaptureListFromLogger(UUID uuid, boolean currentThreadOnly) {
        super(uuid, currentThreadOnly);
    }

    public CaptureListFromLogger(boolean currentThreadOnly) {
        this(UUID.randomUUID(), currentThreadOnly);
    }


    @Override
    public List<LogEvent> get() {
        return getEvents();
    }

    @Override
    protected void accept(LogEvent event) {
        events.add(event.toImmutable());
    }
}
