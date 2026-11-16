package nl.vpro.jmx;

import lombok.extern.log4j.Log4j2;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import nl.vpro.logging.log4j2.CaptureStringFromLogger;
import nl.vpro.util.ThreadPools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;

@Log4j2
@Isolated
@Execution(SAME_THREAD)
class MBeans2Test {
    static {
        ThreadPools.copyExecutor.submit(() -> {}); // Just to early trigger the info about virtual threads in java < 21
    }

    @AfterEach
    public void after() throws InterruptedException {
        assertThat(MBeans.awaitIdle(Duration.ofMinutes(1))).isTrue();
    }


    @Test
    void multiLine() {
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

    void multiLineCapture(boolean currentThreadOnly) {
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
    void multiLineCaptureTimeout(boolean currentThreadOnly) throws InterruptedException {
        try (CaptureStringFromLogger capture =  CaptureStringFromLogger.infoAllThreads()) {
            CountDownLatch release = new CountDownLatch(1);
            CountDownLatch finished = new CountDownLatch(1);
            String result;
            try {
                result = MBeans2.returnMultilineString("test",
                    Duration.ofMillis(10),
                    currentThreadOnly,
                    () -> {
                        log.info("foo bar!");
                        release.await();
                        log.info("pietje puk");
                        finished.countDown();
                    });
            } finally {
                release.countDown();
            }
            assertThat(result).isIn(
                "\n...\nstill busy. Please check logs",
                "foo bar!\n...\nstill busy. Please check logs");
            assertThat(finished.await(1, TimeUnit.MINUTES)).isTrue();
            assertThat(capture.get()).endsWith("""
                foo bar!
                pietje puk
                """);
        }
    }

}
