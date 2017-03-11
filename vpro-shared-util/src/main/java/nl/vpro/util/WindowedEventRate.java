package nl.vpro.util;

import lombok.Builder;

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
public class WindowedEventRate extends Windowed<AtomicLong> {

    @Builder
    public WindowedEventRate(Duration window, Duration bucketDuration, Integer bucketCount) {
        super(window, bucketDuration, bucketCount);
    }

    @Override
    AtomicLong[] newBuckets(int bucketCount) {
        return new AtomicLong[bucketCount];
    }

    @Override
    AtomicLong initialValue() {
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

    public double getRate(TimeUnit unit) {
        shiftBuckets();

        long totalCount = 0;
        for (AtomicLong bucket : buckets) {
            totalCount += bucket.get();
        }

        final long relevantDuration;
        if (isWarmingUp()) {
            relevantDuration = Duration.between(start, Instant.now()).toMillis();
        } else {
            relevantDuration = totalDuration;
        }

        return ((double) totalCount * TimeUnit.MILLISECONDS.convert(1, unit)) / relevantDuration;
    }

    public String toString() {
        return "" + getRate(TimeUnit.SECONDS) + " /s" + (isWarmingUp() ? " (warming up)" : "");
    }



}
