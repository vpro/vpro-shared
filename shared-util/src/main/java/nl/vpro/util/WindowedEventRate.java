package nl.vpro.util;

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

    public WindowedEventRate(int unit, TimeUnit timeUnit, int bucketCount) {
        buckets = new long[bucketCount];
        Arrays.fill(buckets, 0L);
        windowSize = TimeUnit.MILLISECONDS.convert(unit, timeUnit);
        this.bucketCount = bucketCount;
        this.totalDuration = bucketCount * windowSize;
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

    public double getRate(TimeUnit unit) {
        shiftBuckets();
        long totalCount = 0;
        for (long bucket : buckets) {
            totalCount += bucket;
        }
        return ((double) totalCount * TimeUnit.MILLISECONDS.convert(1, unit)) / totalDuration;
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
        return "" + getRate(TimeUnit.SECONDS) + " /s";
    }

    public Duration getTotalDuration() {
        return Duration.ofMillis(totalDuration);
    }

    public boolean isWarmingUp() {
        return Instant.now().isAfter(start.plus(getTotalDuration()));
    }


}
