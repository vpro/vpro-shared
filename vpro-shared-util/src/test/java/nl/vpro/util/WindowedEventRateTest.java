package nl.vpro.util;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Percentage.withPercentage;

/**
 * @author Michiel Meeuwissen
 * @since 0.38
 */
public class WindowedEventRateTest {


    @Test
    public void test() throws InterruptedException {
        WindowedEventRate rate = WindowedEventRate.builder()
            .bucketCount(5)
            .window(Duration.ofSeconds(5)).build();
        long start = System.currentTimeMillis();

        for (int i = 0; i < 1000; i++) {
            System.out.println("duration: " + (System.currentTimeMillis() - start) + " ms. Measured rate " + rate.getRate(TimeUnit.SECONDS) + " #/s (" + rate.isWarmingUp() + ")");
            rate.newEvent();

        }

        Thread.sleep(4800L);

        System.out.println("duration: " + (System.currentTimeMillis() - start) + " ms. Measured rate " + rate.getRate(TimeUnit.SECONDS) + " #/s (" + rate.isWarmingUp() +")");

        Thread.sleep(201L);

        assertThat(rate.isWarmingUp()).isFalse();
        System.out.println(rate.getRanges());
    }


    @Test
    public void testAccuracyDuringWarmup() throws InterruptedException {
        WindowedEventRate rate = WindowedEventRate.builder()
            .window(Duration.ofMillis(1500))
            .bucketCount(50).build();

        assertThat(rate.getTotalDuration()).isEqualByComparingTo(Duration.ofMillis(1500));

        for (int i = 0; i < 10; i++) {
            rate.newEvents(10);
            Thread.sleep(100);
        }
        assertThat(rate.isWarmingUp()).isTrue();
        assertThat(rate.getRate(TimeUnit.SECONDS)).isCloseTo(100.0, withPercentage(20));
        for (int i = 0; i < 10; i++) {
            rate.newEvents(10);
            Thread.sleep(100);
        }
        assertThat(rate.isWarmingUp()).isFalse();
        assertThat(rate.getRate(TimeUnit.SECONDS)).isCloseTo(100.0, withPercentage(20));
    }

}
