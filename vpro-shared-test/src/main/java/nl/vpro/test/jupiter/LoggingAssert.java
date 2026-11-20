package nl.vpro.test.jupiter;

import java.lang.annotation.*;

import org.junit.jupiter.api.extension.ExtendWith;

import nl.vpro.logging.log4j2.CaptureListFromLogger;

/**
 * Asserts on the number of warnings and errors that are logged during the execution of a test.
 * <p>
 * Can be put on a test class (applies to every test in it, also in {@code @Nested} classes) or on a test method (overrides the one on the class).
 * <pre>{@code
 * @LoggingAssert // no warnings, no errors
 * class MyTest {
 *
 *     @Test
 *     @LoggingAssert(warnings = 1)
 *     void expectsOneWarning() {
 *        ...
 *     }
 * }
 * }</pre>
 * The test method may declare a {@link CaptureListFromLogger} parameter to inspect the captured warnings and errors (and e.g. {@link CaptureListFromLogger#clear() clear} them after checking).
 * <p>
 * Only the test method itself is considered (not {@code @BeforeEach}/{@code @AfterEach}). Logging is captured using a {@link CaptureListFromLogger}, so this requires log4j2-core as logging implementation.
 *
 * @author Michiel Meeuwissen
 * @since 5.20
 * @see LoggingAssertExtension
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ExtendWith(LoggingAssertExtension.class)
public @interface LoggingAssert {

    /**
     * Value to indicate that a certain count must not be checked.
     */
    int ANY = -1;

    /**
     * The exact number of events with level {@code WARN} that are expected. {@link #ANY} to not check.
     */
    int warnings() default 0;

    /**
     * The exact number of events with level {@code ERROR} or {@code FATAL} that are expected. {@link #ANY} to not check.
     */
    int errors() default 0;

    /**
     * Whether only logging from the thread executing the test must be considered. If {@code false}, logging of all threads is captured, which may also include logging from other tests running in parallel.
     */
    boolean currentThreadOnly() default true;

    /**
     * Whether to fail immediately when a warning or error is logged that exceeds the expected number. The {@code log} call itself then throws (an {@link org.apache.logging.log4j.core.appender.AppenderLoggingException} wrapping an {@link AssertionError}), so the stack trace shows where it was logged.
     * <p>
     * The code under test may catch that exception. Therefore, the check after the test still happens too.
     */
    boolean failFast() default false;
}
