package nl.vpro.util;

import lombok.Builder;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author Michiel Meeuwissen
 * @since 0.38
 */
public class WindowedEventRate {

    protected final int bucketCount;
    private final long[] buckets;
    private final long windowSize;
    private final long totalDuration;
    private long currentBucketTime = System.currentTimeMillis();
    private int currentBucket = 0;
    private final Instant start = Instant.now();
    private boolean warmingUp = true;

    @Builder
    public WindowedEventRate(Duration windowSize, int windowCount) {
        buckets = new long[windowCount];
        Arrays.fill(buckets, 0L);
        this.windowSize = windowSize.toMillis();
        this.bucketCount = windowCount;
        this.totalDuration = windowCount * this.windowSize;
    }

    public WindowedEventRate(int unit, TimeUnit timeUnit, int bucketCount) {
        this(Duration.ofMillis(TimeUnit.MILLISECONDS.convert(unit, timeUnit)), bucketCount);
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

        final long relevantDuration;
        if (isWarmingUp()) {
            long relevantDurationInMillis = Duration.between(start, Instant.now()).toMillis();
            long numberOfBuckets = relevantDurationInMillis / windowSize;
            relevantDuration = windowSize * numberOfBuckets;
        } else {
            relevantDuration = totalDuration;
        }
        long totalCount = 0;
        for (long bucket : buckets) {
            totalCount += bucket;
        }
        return ((double) totalCount * TimeUnit.MILLISECONDS.convert(1, unit)) / relevantDuration;
    }

    private void shiftBuckets() {
        long currentTime = System.currentTimeMillis();
        long afterBucketBegin = currentTime - currentBucketTime;
        int i = 0;
        while (afterBucketBegin > windowSize && (i++) < buckets.length) {
            currentBucket++;
            currentBucket %= buckets.length;
            buckets[currentBucket] = 0;
            afterBucketBegin -= windowSize;
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
