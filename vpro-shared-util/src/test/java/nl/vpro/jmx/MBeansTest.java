package nl.vpro.jmx;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

import nl.vpro.jmx.MBeans.UpdatableString;
import nl.vpro.logging.simple.*;

import static nl.vpro.jmx.MBeans.multiLine;
import static nl.vpro.jmx.MBeans.returnString;
import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
@SuppressWarnings({"Duplicates", "deprecation", "BusyWait"})
@Slf4j
public class MBeansTest {

    @Test
    public void testWithUpdateableString() {
        log.info("Start");

        UpdatableString string = new UpdatableString(log, "test");

        assertThat(returnString(
            string,
            Duration.ofMillis(1500),
            () -> {
                string.info("starting");
                Thread.sleep(50);
                string.info("first thing");
                Thread.sleep(3000);
                string.info("second thing thing");
                return "ready";

            }
        )).isEqualTo("first thing\n" +
            "...\n" +
            "still busy. Please check logs");
    }

    @Test
    public void testWithStringBuilderLogger() {
        log.info("Start");

        StringSupplierSimpleLogger string  = StringBuilderSimpleLogger.builder()
            .prefix((l) -> "")
            .chain(Slf4jSimpleLogger.of(log));

        assertThat(returnString(
            string,
            Duration.ofMillis(100),
            () -> {
                string.info("starting");
                Thread.sleep(50);
                string.info("first thing");
                Thread.sleep(200);
                string.info("second thing thing");
                return "ready";

            }
        )).isEqualTo("starting\n" +
            "first thing\n" +
            "...\n" +
            "still busy. Please check logs");
    }


    @Test
    public void cancel() throws InterruptedException, ExecutionException {
        returnString("KEY", multiLine(log, "Filling media queues cancel"),  Duration.ofMillis(100), (l) -> {
            int count = 0;
            while(true) {
                try {
                    Thread.sleep(400);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.debug(e.getMessage());
                }
                if (Thread.currentThread().isInterrupted()) {
                    log.info("interrupted");
                    return;
                }
                l.info(count++ + " " + Instant.now());
            }
        });
        assertThat(MBeans.cancel("KEY").get()).containsIgnoringCase("cancel");
        assertThat(MBeans.locks).isEmpty();
    }



    @SuppressWarnings("InfiniteLoopStatement")
    @Test
    public void abandon() throws InterruptedException, ExecutionException {
        final boolean[] started = new boolean[1];
        final List<String> interrupted = new ArrayList<>();
        String r = returnString("KEY", multiLine(log, "Filling media queues abandon"),  Duration.ofMillis(100), (l) -> {
            int count = 0;

            while(true) {
                try {
                    Thread.sleep(400);
                } catch (InterruptedException e) {
                    log.debug(e.getClass().getName() + ":" + e.getMessage());
                    interrupted.add(e.getMessage());
                }
                synchronized (started) {
                    started[0] = true;
                    started.notifyAll();
                }
                // ignoring interrupt, simply proceed
                l.info(count++ + " " + Instant.now());
            }
        });
        log.info(r);
        synchronized (started) {
            while (!started[0]) {
                started.wait();
            }
        }
        assertThat(MBeans.cancel("KEY").get()).containsIgnoringCase("abandon");
        assertThat(MBeans.locks).isEmpty();
        assertThat(interrupted.get(0)).contains("sleep interrupted");
    }


}
