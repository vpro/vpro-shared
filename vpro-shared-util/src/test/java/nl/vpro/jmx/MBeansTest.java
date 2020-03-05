package nl.vpro.jmx;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

import org.junit.Test;

import nl.vpro.logging.simple.Slf4jSimpleLogger;
import nl.vpro.logging.simple.StringBuilderSimpleLogger;
import nl.vpro.logging.simple.StringSupplierSimpleLogger;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
@SuppressWarnings({"Duplicates", "deprecation"})
@Slf4j
public class MBeansTest {

    @Test
    public void testWithUpdateableString() {
        log.info("Start");

        MBeans.UpdatableString string = new MBeans.UpdatableString(log, "test");

        assertThat(MBeans.returnString(
            string,
            Duration.ofMillis(150),
            () -> {
                string.info("starting");
                Thread.sleep(50);
                string.info("first thing");
                Thread.sleep(300);
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

        assertThat(MBeans.returnString(
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

}
