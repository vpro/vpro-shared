package nl.vpro.util;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * An implementation of {@link Windowed} with {@link AtomicLong} values.
 *
 * Keeps track of an event rate in a current window of a given duration
 * E.g. If you want to report a certain event rate average for the last 5 minutes.
 *
 * Every 'bucket' of the window is just counter, and the associated {@link #getWindowValue()} is just the sum.
 *
 * Logically this class also provides {@link #getRate(TimeUnit)}.
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

    @Override
    public AtomicLong getWindowValue() {
        return new AtomicLong(getTotalCount());
    }

    /**
     * The current rate as a number of events per given unit of time
     * @param unit The unit of time to express the rate in.
     */
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

    /**
     * The current rate as a number of events per SI unit of time (the second)
     */
    public double getRate() {
        return getRate(TimeUnit.SECONDS);
    }


    public String toString() {
        return "" + getRate(TimeUnit.SECONDS) + " /s" + (isWarmingUp() ? " (warming up)" : "");
    }



}
