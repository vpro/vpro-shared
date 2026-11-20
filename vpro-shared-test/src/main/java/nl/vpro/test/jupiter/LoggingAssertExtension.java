package nl.vpro.test.jupiter;

import java.lang.reflect.AnnotatedElement;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.jupiter.api.extension.*;
import org.junit.platform.commons.support.AnnotationSupport;

import nl.vpro.logging.log4j2.CaptureListFromLogger;

/**
 * JUnit Jupiter extension implementing {@link LoggingAssert}. Normally there is no need to register it explicitly, since {@link LoggingAssert} is meta-annotated with it.
 * <p>
 * The test method can also declare a parameter of type {@link CaptureListFromLogger}, to inspect the captured events. This capture contains events of level {@code WARN} and higher. Calling {@link CaptureListFromLogger#clear()} on it also resets what is asserted afterward.
 * <p>
 * If the extension is registered explicitly (with {@code @ExtendWith(LoggingAssertExtension.class)}) without {@link LoggingAssert}, the parameter can still be injected, but nothing is asserted.
 *
 * @author Michiel Meeuwissen
 * @since 5.20
 */
public class LoggingAssertExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback, ParameterResolver {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(LoggingAssertExtension.class);

    private static final String CAPTURE = "capture";

    private static final Predicate<LogEvent> WARNING = e -> e.getLevel().equals(Level.WARN);
    private static final Predicate<LogEvent> ERROR = e -> e.getLevel().isMoreSpecificThan(Level.ERROR);

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        if (findAnnotation(context).isPresent()) {
            getOrCreate(context);
        }
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        Capture capture = context.getStore(NAMESPACE).remove(CAPTURE, Capture.class);
        if (capture == null) {
            return;
        }
        List<LogEvent> events;
        try (capture) {
            events = capture.allEvents();
        }
        Optional<LoggingAssert> config = findAnnotation(context);
        if (config.isEmpty() || context.getExecutionException().isPresent()) {
            // nothing to assert, or test failed already (don't obscure that)
            return;
        }
        StringBuilder failures = new StringBuilder();
        check(failures, "warnings", config.get().warnings(), events, WARNING);
        check(failures, "errors", config.get().errors(), events, ERROR);
        if (!failures.isEmpty()) {
            throw new AssertionError(failures.toString().trim());
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return parameterContext.getParameter().getType().equals(CaptureListFromLogger.class)
            && extensionContext.getTestMethod().map(m -> m.equals(parameterContext.getDeclaringExecutable())).orElse(false);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return getOrCreate(extensionContext).capture;
    }

    private static Capture getOrCreate(ExtensionContext context) {
        Optional<LoggingAssert> config = findAnnotation(context);
        return context.getStore(NAMESPACE).computeIfAbsent(CAPTURE, k -> new Capture(config.orElse(null)), Capture.class);
    }

    private static void check(StringBuilder failures, String name, int expected, List<LogEvent> events, Predicate<LogEvent> predicate) {
        if (expected == LoggingAssert.ANY) {
            return;
        }
        List<LogEvent> matching = events.stream().filter(predicate).toList();
        if (matching.size() != expected) {
            failures.append(message(name, expected, matching)).append("\n");
        }
    }

    private static String message(String name, int expected, List<LogEvent> matching) {
        return "Expected " + expected + " " + name + ", but " + matching.size() + " were logged"
            + (matching.isEmpty() ? "" : ":\n")
            + matching.stream().map(LoggingAssertExtension::format).collect(Collectors.joining("\n"));
    }

    private static String format(LogEvent event) {
        return "  " + event.getLevel() + " " + event.getLoggerName() + " - " + event.getMessage().getFormattedMessage()
            + (event.getThrown() == null ? "" : " (" + event.getThrown() + ")");
    }

    /**
     * Finds the closest {@link LoggingAssert}: on the method, on the class, or on an enclosing class (for {@code @Nested} tests).
     */
    private static Optional<LoggingAssert> findAnnotation(ExtensionContext context) {
        ExtensionContext current = context;
        while (current != null) {
            Optional<AnnotatedElement> element = current.getElement();
            if (element.isPresent()) {
                Optional<LoggingAssert> found = AnnotationSupport.findAnnotation(element.get(), LoggingAssert.class);
                if (found.isPresent()) {
                    return found;
                }
            }
            current = current.getParent().orElse(null);
        }
        return Optional.empty();
    }

    /**
     * The {@link CaptureListFromLogger}, plus the events that were rejected because of {@link LoggingAssert#failFast()}. Those are not added to the capture, because the exception is thrown before that.
     */
    private static class Capture implements AutoCloseable {
        private final List<LogEvent> rejected = Collections.synchronizedList(new ArrayList<>());
        private final CaptureListFromLogger capture;

        Capture(LoggingAssert config) {
            CaptureListFromLogger.Builder builder = CaptureListFromLogger.builder()
                .level(Level.WARN)
                .currentThreadOnly(config == null || config.currentThreadOnly());
            if (config != null && config.failFast()) {
                // The predicate is called by the capturing appender, which doesn't ignore exceptions, so this makes the log call itself fail
                builder.predicate(event -> {
                    failFast(event, "warnings", config.warnings(), WARNING);
                    failFast(event, "errors", config.errors(), ERROR);
                    return true;
                });
            }
            this.capture = builder.build();
        }

        private void failFast(LogEvent event, String name, int expected, Predicate<LogEvent> predicate) {
            if (expected == LoggingAssert.ANY || !predicate.test(event)) {
                return;
            }
            List<LogEvent> matching = allEvents().stream().filter(predicate).collect(Collectors.toCollection(ArrayList::new));
            if (matching.size() + 1 > expected) {
                LogEvent immutable = event.toImmutable();
                rejected.add(immutable);
                matching.add(immutable);
                throw new AssertionError(message(name, expected, matching));
            }
        }

        List<LogEvent> allEvents() {
            synchronized (rejected) {
                return Stream.concat(capture.getEvents().stream(), rejected.stream()).toList();
            }
        }

        @Override
        public void close() {
            capture.close();
        }
    }
}
