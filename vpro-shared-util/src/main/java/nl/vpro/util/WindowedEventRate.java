package nl.vpro.util;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Keeps track of an event rate in a current window of a given duration
 * E.g. If you want to report a certain event rate average for the last 5 minutes.
 *
 *
 * @author Michiel Meeuwissen
 * @since 0.38
 */
@Slf4j
public class WindowedEventRate extends Windowed<AtomicLong> {

    /**
     * @param window         The total time window for which events are going to be measured (or <code>null</code> if bucketDuration specified)
     * @param bucketDuration The duration of one bucket (or <code>null</code> if window specified).
     * @param bucketCount    The number of buckets the total window time is to be divided in.
     */
    @Builder
    public WindowedEventRate(
        Duration window,
        Duration bucketDuration,
        Integer bucketCount) {
        super(window, bucketDuration, bucketCount);
    }

    @Override
    protected AtomicLong[] newBuckets(int bucketCount) {
        return new AtomicLong[bucketCount];
    }

    @Override
    protected AtomicLong initialValue() {
        return new AtomicLong(0L);

    }

    public WindowedEventRate(int unit, TimeUnit timeUnit, int bucketCount) {
        this(Duration.ofMillis(
            TimeUnit.MILLISECONDS.convert(unit, timeUnit) * bucketCount),
            null, bucketCount);
    }
    public WindowedEventRate(int unit, TimeUnit timeUnit) {
        this(unit, timeUnit, 100);
    }

    public WindowedEventRate(TimeUnit timeUnit) {
        this(1, timeUnit);
    }

    public void newEvent() {
        currentBucket().incrementAndGet();
    }

    public void newEvents(int count) {
        currentBucket().addAndGet(count);
    }

    public long getTotalCount() {
        shiftBuckets();
        long totalCount = 0;
        for (AtomicLong bucket : buckets) {
            totalCount += bucket.get();
        }
        return totalCount;
    }

    public Duration getRelevantDuration() {
        shiftBuckets();
        if (isWarmingUp()) {
            return Duration.between(start, Instant.now());
        } else {
            return getTotalDuration();
        }
    }

    public double getRate(TimeUnit unit) {
        long totalCount = getTotalCount();
        Duration relevantDuration = getRelevantDuration();
        return ((double) totalCount * TimeUnit.NANOSECONDS.convert(1, unit)) / relevantDuration.toNanos();
    }

    public String toString() {
        return "" + getRate(TimeUnit.SECONDS) + " /s" + (isWarmingUp() ? " (warming up)" : "");
    }



}
