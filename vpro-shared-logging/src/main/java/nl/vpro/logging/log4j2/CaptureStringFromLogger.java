package nl.vpro.logging.log4j2;

import lombok.extern.log4j.Log4j2;

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
 * It on default only captures log statements originating from the thread it was instantiated in.
 * There are two ways to do that differently:
 * <ol>
 *  <li> Call {@link #associateWithCurrentThread()} to associate the capture with another thread too.
 *  <li> Instantiate with 'currentThreadOnly' false.
 * </ol>
 *
 *
 * @author Michiel Meeuwissen
 * @since 5.10
 */
@Log4j2
public class CaptureStringFromLogger extends AbstractCaptureLogger implements Supplier<String> {

    private final StringBuilderWriter writer;
    private final WriterAppender appender;

    public CaptureStringFromLogger() {
        this("%d{ISO8601}{Europe/Amsterdam}\t%msg%n", Level.INFO);
    }

    public CaptureStringFromLogger(String pattern, Level level) {
        this(pattern, level, new StringBuilder(), true);
    }

    public static CaptureStringFromLogger info(String pattern) {
        return new CaptureStringFromLogger(pattern, Level.INFO, new StringBuilder(), true);
    }

    public static CaptureStringFromLogger info() {
        return info("%msg%n");
    }

    public static CaptureStringFromLogger infoAllThreads(String pattern) {
        return new CaptureStringFromLogger(pattern, Level.INFO, new StringBuilder(), false);
    }
    public static CaptureStringFromLogger infoAllThreads() {
        return infoAllThreads("%msg%n");
    }


    @lombok.Builder
    private CaptureStringFromLogger(String pattern, Level level, StringBuilder builder, Boolean currentThreadOnly) {
        super(currentThreadOnly == null || currentThreadOnly);
        this.writer = new StringBuilderWriter(builder == null ? new StringBuilder() : builder);
        this.appender = WriterAppender.newBuilder()
            .setTarget(writer)
            .setIgnoreExceptions(false)
            .setFollow(true)
            .setName(uuid.toString())
            .setLayout(PatternLayout.newBuilder()
                .withPattern(pattern)
                .build()
            )
            .build();
    }

    @Override
    protected void accept(LogEvent logEvent) {
        appender.append(logEvent);
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
