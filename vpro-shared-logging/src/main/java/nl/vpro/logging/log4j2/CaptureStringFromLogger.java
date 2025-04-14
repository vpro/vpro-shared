package nl.vpro.logging.log4j2;

import lombok.extern.log4j.Log4j2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.WriterAppender;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.util.StringBuilderWriter;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Simple setup using log4j2 to temporary capture logging and collect it to a String.
 * <p>
 * usage
 * <pre>{@code
 *
 *   try (CaptureStringFromLogger capture = new CaptureStringFromLogger()) {
 *
 *       stuff, which might log some things (as long as in same thread!)
 *
 *       publication.setMessages(capture.get())
 *   }
 * }
 * </pre>
 *
 *
 * @author Michiel Meeuwissen
 */
@Log4j2
public class CaptureStringFromLogger implements AutoCloseable, Supplier<String> {

    static final ThreadLocal<Boolean> threadLocal = ThreadLocal.withInitial(() -> false);

    private final StringBuilderWriter writer = new StringBuilderWriter();
    private final WriterAppender writerAppender;

    public CaptureStringFromLogger() {
        UUID uuid = UUID.randomUUID();
        threadLocal.set(true);
        writerAppender = WriterAppender.newBuilder()
            .setTarget(writer)
            .setIgnoreExceptions(false)
            .setFilter(new AbstractFilter() {
                @Override
                public Result filter(LogEvent event) {
                    return threadLocal.get()  ? Result.NEUTRAL : Result.DENY;
                }
            })
            .setFollow(true)
            .setLayout(PatternLayout.newBuilder().withPattern("%msg%n").build())
            .setName("" + uuid)
            .build();
        if (LogManager.getRootLogger() instanceof Logger logger) {
            writerAppender.start();
            logger.addAppender(writerAppender);
        } else {
            log.info("Current logging implementation is not log4j2-core");
        }
    }

    @Override
    public void close() {
        threadLocal.remove();
        if (LogManager.getRootLogger() instanceof Logger logger) {
            logger.removeAppender(writerAppender);
        }
    }

    @Override
    public String get() {
        writer.flush();
        return writer.toString().strip();
    }
}
