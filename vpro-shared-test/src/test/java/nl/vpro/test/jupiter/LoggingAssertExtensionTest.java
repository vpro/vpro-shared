package nl.vpro.test.jupiter;

import lombok.extern.log4j.Log4j2;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.testkit.engine.EngineTestKit;

import nl.vpro.logging.log4j2.CaptureListFromLogger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.*;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;

class LoggingAssertExtensionTest {

    @Test
    void cases() {
        EngineTestKit.engine("junit-jupiter")
            .selectors(selectClass(Cases.class))
            .execute()
            .testEvents()
            .assertStatistics(stats -> stats.succeeded(5).failed(3))
            .assertThatEvents()
            .haveExactly(1, event(test("unexpectedWarning"), finishedWithFailure(instanceOf(AssertionError.class),
                message(m -> m.contains("Expected 0 warnings, but 1 were logged") && m.contains("WARN") && m.contains("a warning")))))
            .haveExactly(1, event(test("unexpectedError"), finishedWithFailure(instanceOf(AssertionError.class),
                message(m -> m.contains("Expected 0 errors, but 1 were logged")))))
            .haveExactly(1, event(test("missingWarning"), finishedWithFailure(instanceOf(AssertionError.class),
                message(m -> m.contains("Expected 1 warnings, but 0 were logged")))))
            ;
    }

    @Test
    void failingTestIsNotObscured() {
        EngineTestKit.engine("junit-jupiter")
            .selectors(selectClass(FailingCase.class))
            .execute()
            .testEvents()
            .assertThatEvents()
            .haveExactly(1, event(test("fails"), finishedWithFailure(instanceOf(IllegalStateException.class))));
    }

    @Log4j2
    @LoggingAssert
    static class Cases {
        @Test
        void clean() {
            log.info("just info");
        }

        @Test
        void unexpectedWarning() {
            log.warn("a warning");
        }

        @Test
        void unexpectedError() {
            log.error("an error");
        }

        @Test
        @LoggingAssert(warnings = 1, errors = 2)
        void expected() {
            log.warn("a warning");
            log.error("an error");
            log.fatal("a fatal");
        }

        @Test
        @LoggingAssert(warnings = 1)
        void missingWarning() {
        }

        @Test
        @LoggingAssert(warnings = LoggingAssert.ANY)
        void anyWarnings() {
            log.warn("a warning");
            log.warn("another warning");
        }

        @Nested
        class Inner {
            @Test
            void cleanInNested() {
                log.info("info");
            }

            @Test
            @LoggingAssert(errors = 1)
            void errorInNested() {
                log.error("error");
            }
        }
    }

    @Log4j2
    @LoggingAssert
    static class FailingCase {
        @Test
        void fails() {
            log.error("an error");
            throw new IllegalStateException();
        }
    }

    @Test
    void parameters() {
        EngineTestKit.engine("junit-jupiter")
            .selectors(selectClass(ParameterCases.class), selectClass(ParameterWithoutAnnotation.class))
            .execute()
            .testEvents()
            .assertStatistics(stats -> stats.succeeded(3).failed(1))
            .assertThatEvents()
            .haveExactly(1, event(test("injectedButStillAsserted"), finishedWithFailure(instanceOf(AssertionError.class),
                message(m -> m.contains("Expected 0 warnings, but 1 were logged")))));
    }

    @Log4j2
    @LoggingAssert
    static class ParameterCases {
        @Test
        @LoggingAssert(warnings = 1)
        void inspect(CaptureListFromLogger capture) {
            log.info("info");
            log.warn("a warning");
            assertThat(capture.getEvents()).hasSize(1);
            assertThat(capture.getEvents().get(0).getMessage().getFormattedMessage()).isEqualTo("a warning");
        }

        @Test
        void clearResets(CaptureListFromLogger capture) {
            log.warn("an expected warning");
            assertThat(capture.getEvents()).hasSize(1);
            capture.clear();
        }

        @Test
        void injectedButStillAsserted(CaptureListFromLogger capture) {
            log.warn("a warning");
        }
    }

    @Log4j2
    @ExtendWith(LoggingAssertExtension.class)
    static class ParameterWithoutAnnotation {
        @Test
        void noAssertions(CaptureListFromLogger capture) {
            log.error("an error");
            assertThat(capture.getEvents()).hasSize(1);
        }
    }

    @Test
    void annotationIsMetaAnnotated() {
        assertThat(LoggingAssert.class.getAnnotation(org.junit.jupiter.api.extension.ExtendWith.class).value())
            .containsExactly(LoggingAssertExtension.class);
    }
}
