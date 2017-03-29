package nl.vpro.util;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

import com.google.common.collect.Range;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Percentage.withPercentage;

/**
 * @author Michiel Meeuwissen
 * @since 0.38
 */
public class WindowedEventRateTest {


    @Test
    public void testBuckets() throws InterruptedException {
        WindowedEventRate rate = WindowedEventRate.builder()
            .bucketCount(5)
            .bucketDuration(Duration.ofSeconds(1)).build();

        assertThat(rate.getBucketDuration().toMillis()).isEqualTo(1000);
        rate.newEvent();
        Thread.sleep(1001);
        rate.newEvents(2);

        AtomicLong[] buckets = rate.getBuckets();
        assertThat(buckets[buckets.length - 1].get()).isEqualTo(2); // current bucket
        assertThat(buckets[buckets.length - 2].get()).isEqualTo(1); // one bucketDuration ago
        assertThat(buckets[buckets.length - 3].get()).isEqualTo(0); // longer ago

        List<Map.Entry<Range<Instant>, AtomicLong>> ranges =
            new ArrayList<>(rate.getRanges().entrySet());
        assertThat(ranges.get(buckets.length - 1).getValue().longValue()).isEqualTo(2); // current bucket
        assertThat(ranges.get(buckets.length - 2).getValue().longValue()).isEqualTo(1); // one buckedDuration ago
        assertThat(ranges.get(buckets.length - 3).getValue().longValue()).isEqualTo(0); // longer ago

        assertThat(ranges.get(buckets.length - 2).getKey()
            .lowerEndpoint()).isEqualByComparingTo(rate.getStart()); // This bucket was the first one

        for (int i = 0; i < ranges.size() - 1; i++) {
            assertThat(ranges.get(i).getKey().lowerEndpoint())
                .isLessThanOrEqualTo(ranges.get(i + 1).getKey().lowerEndpoint());
            assertThat(ranges.get(i).getKey().upperEndpoint())
                .isEqualByComparingTo(ranges.get(i + 1).getKey().lowerEndpoint());
        }

    }


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

        assertThat(rate.getBuckets()).hasSize(50);
        assertThat(rate.getTotalDuration()).isEqualByComparingTo(Duration.ofMillis(1500));

        for (int i = 0; i < 10; i++) {
            assertThat(rate.getTotalCount()).isEqualTo(i * 10);
            rate.newEvents(10);
            Thread.sleep(100);
        }
        // got 100 events in about 1 second.
        assertThat(rate.isWarmingUp()).isTrue();
        assertThat(rate.getTotalCount()).isEqualTo(100);
        assertThat(rate.getRelevantDuration().toMillis()).isCloseTo(1000, withPercentage(20));

        double rateDuringWarmup = rate.getRate(TimeUnit.SECONDS);
        System.out.println(rateDuringWarmup + " ~ 100 /s");

        for (int i = 0; i < 10; i++) {
            rate.newEvents(10);
            Thread.sleep(100);
        }
        assertThat(rate.isWarmingUp()).isFalse();
        Long relevantDuration = rate.getRelevantDuration().toMillis();
        assertThat(relevantDuration).isLessThanOrEqualTo(1500);
        assertThat(relevantDuration).isGreaterThan(1500 - rate.getBucketDuration().toMillis());

        double rateAfterWarmup = rate.getRate(TimeUnit.SECONDS);
        System.out.println(rateAfterWarmup + " ~ 100 /s");
        assertThat(rateAfterWarmup).isCloseTo(100.0, withPercentage(20));

        assertThat(rateDuringWarmup).isCloseTo(100.0, withPercentage(20));
    }

}
