package nl.vpro.logging.log4j2;

import lombok.extern.log4j.Log4j2;

import java.util.UUID;
import java.util.function.Supplier;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.WriterAppender;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.util.StringBuilderWriter;

/**
 * Simple setup using log4j2 to temporarily capture logging and collect it to a String.
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
 * @since 5.10
 */
@Log4j2
public class CaptureStringFromLogger implements AutoCloseable, Supplier<String> {

    static final ThreadLocal<UUID> threadLocal = ThreadLocal.withInitial(() -> null);


    private final StringBuilderWriter writer;
    private final WriterAppender writerAppender;


    public CaptureStringFromLogger() {
        this("%d{ISO8601}{Europe/Amsterdam}\t%msg%n", Level.INFO);
    }
    public CaptureStringFromLogger(String pattern, Level level) {
        this(pattern, level, null);
    }

    @lombok.Builder
    private CaptureStringFromLogger(String pattern, Level level, StringBuilder builder) {
        final UUID uuid = UUID.randomUUID();
        threadLocal.set(uuid);
        this.writer = new StringBuilderWriter(builder);
        writerAppender = WriterAppender.newBuilder()
            .setTarget(writer)
            .setIgnoreExceptions(false)
            .setFilter(new AbstractFilter() {
                @Override
                public Result filter(LogEvent event) {
                    return uuid.equals(threadLocal.get())  && (event.getLevel().isMoreSpecificThan(level)) ? Result.NEUTRAL : Result.DENY;
                }
            })
            .setFollow(true)
            .setLayout(PatternLayout.newBuilder()
                .withPattern(pattern)
                .build())
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

    public StringBuilder getBuilder() {
        return writer.getBuilder();
    }

    @Override
    public String get() {
        writer.flush();
        return writer.toString();
    }
}
