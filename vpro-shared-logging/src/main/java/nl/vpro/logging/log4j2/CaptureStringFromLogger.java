package nl.vpro.logging.log4j2;

import lombok.extern.log4j.Log4j2;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.WriterAppender;
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
public class CaptureStringFromLogger extends AbstractCaptureLogger<CaptureStringFromLogger.AppenderAndWriter> implements Supplier<String> {

    StringBuilderWriter writer;

    public CaptureStringFromLogger() {
        this("%d{ISO8601}{Europe/Amsterdam}\t%msg%n", Level.INFO);
    }

    public CaptureStringFromLogger(String pattern, Level level) {
        this(pattern, level, new StringBuilder());
    }

    @lombok.Builder
    private CaptureStringFromLogger(String pattern, Level level, StringBuilder builder) {
        super(createConsumer(pattern, level, builder));
        this.writer = consumer.writer();

    }


    private static AppenderAndWriter createConsumer(String pattern, Level level, StringBuilder builder) {
        if (builder == null) {
            builder = new StringBuilder();
        }
        StringBuilderWriter writer = new StringBuilderWriter(builder);
        WriterAppender appender = WriterAppender.newBuilder()
            .setTarget(writer)
            .setIgnoreExceptions(false)
            .setFollow(true)
            .setName(UUID.randomUUID().toString())
            .setLayout(PatternLayout.newBuilder()
                .withPattern(pattern)
                .build())
            .build();
        return new AppenderAndWriter(appender, writer);
    }

    public StringBuilder getBuilder() {
        return writer.getBuilder();
    }

    @Override
    public String get() {
        writer.flush();
        return writer.toString();
    }

    record AppenderAndWriter(WriterAppender appender, StringBuilderWriter writer) implements Consumer<LogEvent> {
        public AppenderAndWriter(WriterAppender appender, StringBuilder builder) {
            this(appender, new StringBuilderWriter(builder));
        }

        @Override
        public void accept(LogEvent logEvent) {
            appender.append(logEvent);
        }
    }
}
