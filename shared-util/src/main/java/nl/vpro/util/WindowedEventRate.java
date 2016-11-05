package nl.vpro.util;

import lombok.Builder;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Keeps track of an event rate in a current window of a given duration
 * E.g. If you want to report a certain event rate average for the last 5 minutes.
 *
 *
 * @author Michiel Meeuwissen
 * @since 0.38
 */
public class WindowedEventRate {

    protected final int bucketCount;
    private final long[] buckets;
    private final long bucketDuration;
    private final long totalDuration;
    private long currentBucketTime = System.currentTimeMillis();
    private int currentBucket = 0;
    private final Instant start = Instant.now();
    private boolean warmingUp = true;

    @Builder
    public WindowedEventRate(Duration window, Integer bucketCount) {
        this.bucketCount = bucketCount == null ? 20 : bucketCount;
        buckets = new long[this.bucketCount];
        Arrays.fill(buckets, 0L);
        long tempTotalDuration = window == null ? Duration.ofMinutes(5).toMillis() : window.toMillis();
        this.bucketDuration = tempTotalDuration / this.bucketCount;
        this.totalDuration = this.bucketDuration * this.bucketCount;


    }


    public WindowedEventRate(int unit, TimeUnit timeUnit, int bucketCount) {
        this(Duration.ofMillis(TimeUnit.MILLISECONDS.convert(unit, timeUnit) * bucketCount), bucketCount);
    }
    public WindowedEventRate(int unit, TimeUnit timeUnit) {
        this(unit, timeUnit, 100);
    }

    public WindowedEventRate(TimeUnit timeUnit) {
        this(1, timeUnit);
    }


    public void newEvent() {
        shiftBuckets();
        buckets[currentBucket]++;
    }

    public void newEvents(int count) {
        shiftBuckets();
        buckets[currentBucket]+= count;
    }

    public double getRate(TimeUnit unit) {
        shiftBuckets();

        long totalCount = 0;
        for (long bucket : buckets) {
            totalCount += bucket;
        }

        final long relevantDuration;
        if (isWarmingUp()) {
            relevantDuration = Duration.between(start, Instant.now()).toMillis();
        } else {
            relevantDuration = totalDuration;
        }

        return ((double) totalCount * TimeUnit.MILLISECONDS.convert(1, unit)) / relevantDuration;
    }

    private void shiftBuckets() {
        long currentTime = System.currentTimeMillis();
        long afterBucketBegin = currentTime - currentBucketTime;
        int i = 0;
        while (afterBucketBegin > bucketDuration && (i++) < buckets.length) {
            currentBucket++;
            currentBucket %= buckets.length;
            buckets[currentBucket] = 0;
            afterBucketBegin -= bucketDuration;
            currentBucketTime = currentTime;
        }
    }

    public String toString() {
        return "" + getRate(TimeUnit.SECONDS) + " /s" + (isWarmingUp() ? " (warming up)" : "");
    }

    public Duration getTotalDuration() {
        return Duration.ofMillis(totalDuration);
    }

    public boolean isWarmingUp() {
        if (warmingUp) {
            warmingUp = Instant.now().isBefore(start.plus(getTotalDuration()));
        }
        return warmingUp;
    }


}
