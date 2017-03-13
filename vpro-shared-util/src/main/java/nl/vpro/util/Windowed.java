package nl.vpro.util;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.SortedMap;
import java.util.TreeMap;

import com.google.common.collect.Range;

/**
 * Maintains values duration a certain time window. This window is divided up in a certain number of 'buckets', of which every time the oldest bucket expires and is discarded.
 * The idea is that the values in the buckets can be used to calculate averages which are based on sufficiently long times, though sufficiently sensitive for changes.
 *
 * @author Michiel Meeuwissen
 * @since 1.66
 */
@Slf4j
public abstract class Windowed<T> {

    protected final T[] buckets;
    protected final long bucketDuration;
    protected final long totalDuration;
    protected final Instant start = Instant.now();

    private boolean warmingUp = true;
    protected long currentBucketTime = System.currentTimeMillis();
    protected int currentBucket = 0;

    /**
     * @param window         The total time window for which events are going to be measured (or <code>null</code> if bucketDuration specified)
     * @param bucketDuration The duration of one bucket (or <code>null</code> if window specified).
     * @param bucketCount    The number of buckets the total window time is to be divided in.
     */
    protected Windowed(
        Duration window,
        Duration bucketDuration,
        Integer bucketCount
        ) {
        int bucketCount1 = bucketCount == null ? 20 : bucketCount;
        buckets = newBuckets(bucketCount1);
        for (int i = 0; i < buckets.length; i++) {
            buckets[i] = initialValue();
        }
        if (window == null && bucketDuration == null) {
            window = Duration.ofMinutes(5);
        }
        if (window != null) {
            long tempTotalDuration = window.toMillis();
            this.bucketDuration = tempTotalDuration / bucketCount1;
            this.totalDuration = this.bucketDuration * bucketCount1;
            if (bucketDuration != null && this.bucketDuration != bucketDuration.toMillis()) {
                throw new IllegalArgumentException();

            }
        } else {
            this.bucketDuration = bucketDuration.toMillis();
            this.totalDuration = this.bucketDuration * bucketCount1;
        }
    }

    abstract T[] newBuckets(int bucketCount);

    abstract T initialValue();

    protected T currentBucket() {
        shiftBuckets();
        return buckets[currentBucket];
    }

    public Duration getTotalDuration() {
        return Duration.ofMillis(totalDuration);
    }
    public Duration getBucketDuration() {
        return Duration.ofMillis(bucketDuration);
    }

    public boolean isWarmingUp() {
        if (warmingUp) {
            warmingUp = Instant.now().isBefore(start.plus(getTotalDuration()));
        }
        return warmingUp;
    }

    /**
     * Returns the current buckets, ordered by time. This means that the first one is the oldest one, and the last one is
     * the newest (current) one.
     */
    public T[] getBuckets() {
        shiftBuckets();
        T[] result = newBuckets(buckets.length);
        int j = buckets.length;
        for (int i = currentBucket; i > currentBucket - buckets.length; i--) {
            result[--j] = buckets[(buckets.length + i) % buckets.length];
        }
        return result;
    }

    /**
     * Returns the current buckets, as a map, where the keys are the period to which they apply.
     * @return SortedMap with the oldest buckets first.
     */
    public SortedMap<Range<Instant>, T> getRanges() {
        shiftBuckets();
        Instant now = Instant.now();
        SortedMap<Range<Instant>, T> result = new TreeMap<>(Comparator.comparing(Range::lowerEndpoint));
        Instant end = now;
        int j = 0;
        for (int i = currentBucket; i > currentBucket - buckets.length; i--) {
            Instant begin = end.minusMillis((++j) * bucketDuration);
            result.put(Range.closedOpen(begin, end), buckets[(buckets.length + i) % buckets.length]);
            end = begin;
        }
        return result;

    }


    protected void shiftBuckets() {
        long currentTime = System.currentTimeMillis();
        long afterBucketBegin = currentTime - currentBucketTime;
        int i = 0;
        while (afterBucketBegin > bucketDuration && (i++) < buckets.length) {
            currentBucket++;
            //log.debug("Shifting buckets");
            currentBucket %= buckets.length;
            buckets[currentBucket] = initialValue();
            afterBucketBegin -= bucketDuration;
            currentBucketTime = currentTime;
        }
    }


}
