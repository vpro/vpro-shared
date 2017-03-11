package nl.vpro.util;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.SortedMap;
import java.util.TreeMap;

import com.google.common.collect.Range;

/**
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

    protected Windowed(
        Duration window,
        Duration bucketDuration,
        Integer bucketCount
        ) {
        int bucketCount1 = bucketCount == null ? 20 : bucketCount;
        buckets = newBuckets(bucketCount1);
        Arrays.fill(buckets, initialValue());
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

    public T[] getBuckets() {
        shiftBuckets();
        T[] result = newBuckets(buckets.length);
        for (int i = currentBucket; i < currentBucket + buckets.length; i++) {
            result[i - currentBucket] = buckets[i % buckets.length];
        }
        return result;
    }
    public SortedMap<Range<Instant>, T> getRanges() {
        shiftBuckets();
        Instant now = Instant.now();
        SortedMap<Range<Instant>, T> result = new TreeMap<>(Comparator.comparing(Range::lowerEndpoint));
        Instant begin = now;
        for (int i = currentBucket; i < currentBucket + buckets.length; i++) {
            Instant end = begin;
            begin = end.minusMillis((i - currentBucket) * bucketDuration);
            result.put(Range.closedOpen(begin, end), buckets[i % buckets.length]);
            begin = end;
        }
        return result;

    }


    protected void shiftBuckets() {
        long currentTime = System.currentTimeMillis();
        long afterBucketBegin = currentTime - currentBucketTime;
        int i = 0;
        while (afterBucketBegin > bucketDuration && (i++) < buckets.length) {
            currentBucket++;
            log.debug("Shifting buckets");
            currentBucket %= buckets.length;
            buckets[currentBucket] = initialValue();
            afterBucketBegin -= bucketDuration;
            currentBucketTime = currentTime;
        }
    }


}
