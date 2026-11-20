package nl.vpro.test.jupiter;

import java.lang.reflect.AnnotatedElement;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        if (findAnnotation(context).isPresent()) {
            getOrCreateCapture(context);
        }
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        CaptureListFromLogger capture = context.getStore(NAMESPACE).remove(CAPTURE, CaptureListFromLogger.class);
        if (capture == null) {
            return;
        }
        List<LogEvent> events;
        try (capture) {
            events = capture.getEvents();
        }
        Optional<LoggingAssert> config = findAnnotation(context);
        if (config.isEmpty() || context.getExecutionException().isPresent()) {
            // nothing to assert, or test failed already (don't obscure that)
            return;
        }
        StringBuilder failures = new StringBuilder();
        check(failures, "warnings", config.get().warnings(), events, e -> e.getLevel().equals(Level.WARN));
        check(failures, "errors", config.get().errors(), events, e -> e.getLevel().isMoreSpecificThan(Level.ERROR));
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
        return getOrCreateCapture(extensionContext);
    }

    private static CaptureListFromLogger getOrCreateCapture(ExtensionContext context) {
        boolean currentThreadOnly = findAnnotation(context).map(LoggingAssert::currentThreadOnly).orElse(true);
        return context.getStore(NAMESPACE).computeIfAbsent(CAPTURE, k ->
            CaptureListFromLogger.builder()
                .level(Level.WARN)
                .currentThreadOnly(currentThreadOnly)
                .build(),
            CaptureListFromLogger.class);
    }

    private static void check(StringBuilder failures, String name, int expected, List<LogEvent> events, Predicate<LogEvent> predicate) {
        if (expected == LoggingAssert.ANY) {
            return;
        }
        List<LogEvent> matching = events.stream().filter(predicate).toList();
        if (matching.size() != expected) {
            failures.append("Expected ").append(expected).append(" ").append(name)
                .append(", but ").append(matching.size()).append(" were logged")
                .append(matching.isEmpty() ? "" : ":\n")
                .append(matching.stream().map(LoggingAssertExtension::format).collect(Collectors.joining("\n")))
                .append("\n");
        }
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
}
