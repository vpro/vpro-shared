package nl.vpro.util;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

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
    @lombok.Builder(builderClassName = "Builder")
    private WindowedEventRate(
        Duration window,
        Duration bucketDuration,
        Integer bucketCount,
        Consumer<WindowedEventRate> reporter
        ) {
        super(window, bucketDuration, bucketCount);
        if (reporter != null) {
            ThreadPools.backgroundExecutor.scheduleAtFixedRate(
                () -> {
                    try {
                        reporter.accept(WindowedEventRate.this);
                    } catch (Throwable t) {
                        log.error(t.getMessage(), t);
                    }
            }, 0, this.bucketDuration, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    protected AtomicLong[] newBuckets(int bucketCount) {
        return new AtomicLong[bucketCount];
    }

    @Override
    protected AtomicLong initialValue() {
        return new AtomicLong(0L);
    }

    @Override
    protected boolean resetValue(AtomicLong value) {
        value.set(0);
        return true;
    }

    public WindowedEventRate(int unit, TimeUnit timeUnit, int bucketCount) {
        this(Duration.ofMillis(
            TimeUnit.MILLISECONDS.convert(unit, timeUnit) * bucketCount),
            null, bucketCount, null);
    }
    public WindowedEventRate(int unit, TimeUnit timeUnit) {
        this(unit, timeUnit, 100);
    }

    public WindowedEventRate(TimeUnit timeUnit) {
        this(1, timeUnit);
    }

    public void newEvent() {
        currentBucket().getAndIncrement();
    }

    public void newEvents(int count) {
        currentBucket().getAndAdd(count);
    }

    public long getTotalCount() {
        shiftBuckets();
        long totalCount = 0;
        for (AtomicLong bucket : buckets) {
            totalCount += bucket.get();
        }
        return totalCount;
    }

    /**
     * Returns the current duration of the complete window
     * If we are warming up, then this will be the time since we started.
     * Otherwise only the current bucket is 'warming up', and the
     * relevant duration will be less than the configured 'window', but more than
     * the given window minus the duration of one bucket.
     */
    public Duration getRelevantDuration() {
        shiftBuckets();
        if (isWarmingUp()) {
            return Duration.between(start, Instant.now());
        } else {
            return Duration.ofMillis(
                (buckets.length -1) * bucketDuration // archived buckets (all but one, the current bucket)
                    +
                    System.currentTimeMillis() - currentBucketTime // current bucket is not yet complete
            );
        }
    }

    public double getRate(TimeUnit unit) {
        long totalCount = getTotalCount();
        Duration relevantDuration = getRelevantDuration();
        return ((double) totalCount * TimeUnit.NANOSECONDS.convert(1, unit)) / relevantDuration.toNanos();
    }

    public double getRate(Duration perInterval) {
        long totalCount = getTotalCount();
        Duration relevantDuration = getRelevantDuration();
        return ((double) totalCount * perInterval.toNanos()) / relevantDuration.toNanos();
    }

    public String toString() {
        return "" + getRate(TimeUnit.SECONDS) + " /s" + (isWarmingUp() ? " (warming up)" : "");
    }



}
