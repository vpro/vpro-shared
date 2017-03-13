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
    protected final Instant start = Instant.now(); //.truncatedTo(ChronoUnit.SECONDS);

    private boolean warmingUp = true;
    protected long currentBucketTime = start.toEpochMilli();
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


    /**
     * The total duration, or 'window' we are looking at.
     */
    public Duration getTotalDuration() {
        return Duration.ofMillis(totalDuration);
    }

    /**
     * The duration of one bucket
     */
    public Duration getBucketDuration() {
        return Duration.ofMillis(bucketDuration);
    }

    /**
     * The number of buckets this window is divided in
     */
    public int getBucketCount() {
        return buckets.length;
    }

    /**
     * At which instant the measurements started
     */
    public Instant getStart() {
        return start;
    }

    /**
     * We are still warming up, if since {@link #getStart()} not yet {@link #getTotalDuration()} has elapsed
     */
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
        for (int i = 0; i < buckets.length; i++) {
            result[buckets.length - 1 - i] = buckets[(currentBucket - i + buckets.length) % buckets.length];
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
        Instant end = Instant.ofEpochMilli(currentBucketTime)
            .plusMillis(bucketDuration);
        for (int i = 0; i < buckets.length; i++) {
            Instant begin = end.minusMillis(bucketDuration);
            result.put(Range.closedOpen(begin, end), buckets[(currentBucket - i + buckets.length) % buckets.length]);
            end = begin;
        }
        return result;

    }


    abstract T[] newBuckets(int bucketCount);

    abstract T initialValue();

    /**
     * If values can be reset, this method can do it.
     * @param value to reset if possible
     * @return <code>true</code> if the value was reset. <code>false</code> otherwise and a new {@link #initialValue()} will be used
     */
    protected boolean resetValue(T value) {
        return false;
    }

    protected T currentBucket() {
        shiftBuckets();
        return buckets[currentBucket];
    }

    protected void shiftBuckets() {
        long currentTime = System.currentTimeMillis();
        long afterBucketBegin = currentTime - currentBucketTime;
        int i = 0;
        while (afterBucketBegin > bucketDuration && (i++) < buckets.length) {
            currentBucket++;
            //log.debug("Shifting buckets");
            currentBucket %= buckets.length;
            if (!resetValue(buckets[currentBucket])) {
                buckets[currentBucket] = initialValue();
            }
            afterBucketBegin -= bucketDuration;
            currentBucketTime += bucketDuration;
        }
    }


}
