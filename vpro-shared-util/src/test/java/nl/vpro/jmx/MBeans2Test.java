package nl.vpro.jmx;

import lombok.extern.log4j.Log4j2;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import nl.vpro.logging.log4j2.CaptureStringFromLogger;

import static org.assertj.core.api.Assertions.assertThat;

@Log4j2
@Isolated
class MBeans2Test {


    @Test
    public void multiLine() {
        try (CaptureStringFromLogger capture =  CaptureStringFromLogger.info()) {
            String result  = MBeans2.returnMultilineString(log, (logging) -> {
                capture.associateWithCurrentThread();
                logging.info("foo bar!");
            });
            assertThat(result).isEqualTo("foo bar!");
            assertThat(capture.get()).isEqualTo("foo bar!\n");
        }
    }


    @ParameterizedTest
    @ValueSource(booleans = {true, false})

    public void multiLineCapture(boolean currentThreadOnly) {
        try (CaptureStringFromLogger capture =  CaptureStringFromLogger.infoAllThreads()) {

            String result = MBeans2.returnMultilineString("test",
                Duration.ofSeconds(1),
                currentThreadOnly,
                () -> {
                    log.info("foo bar!");
                    log.debug("debug line");
                    log.info("pietje puk");
                }
            );
            assertThat(result).isEqualTo("""
                foo bar!
                pietje puk""");
            assertThat(capture.get()).endsWith("""
                foo bar!
                pietje puk
                """);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void multiLineCaptureTimeout(boolean currentThreadOnly) throws InterruptedException {
        try (CaptureStringFromLogger capture =  CaptureStringFromLogger.infoAllThreads()) {

            String result = MBeans2.returnMultilineString("test",
                Duration.ofMillis(10),
                currentThreadOnly,
                () -> {
                    log.info("foo bar!");
                    Thread.sleep(100);
                    log.info("pietje puk");
                });
            assertThat(result).isEqualTo("""
                            foo bar!
                            ...
                            still busy. Please check logs""");
            Thread.sleep(200); // wait for logging to be captured
            assertThat(capture.get()).endsWith("""
                foo bar!
                pietje puk
                """);
        }
    }

}
