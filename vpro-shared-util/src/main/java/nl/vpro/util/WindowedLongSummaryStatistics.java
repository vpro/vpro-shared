package nl.vpro.util;

import java.time.Duration;
import java.util.Arrays;
import java.util.LongSummaryStatistics;
import java.util.function.LongConsumer;

/**
 * {@link LongSummaryStatistics} can be aggregated, and therefor {@link Windowed}.
 * @see WindowedDoubleSummaryStatistics
 * @author Michiel Meeuwissen
 * @since 1.66
 */
@Deprecated
public class WindowedLongSummaryStatistics extends Windowed<LongSummaryStatistics> implements LongConsumer {

    @lombok.Builder(builderClassName = "Builder")
    protected WindowedLongSummaryStatistics(
        Duration window,
        Duration bucketDuration,
        Integer bucketCount) {
        super(window, bucketDuration, bucketCount);
    }



    @Override
    protected LongSummaryStatistics[] newBuckets(int bucketCount) {
        return new LongSummaryStatistics[bucketCount];

    }

    @Override
    protected LongSummaryStatistics initialValue() {
        return new LongSummaryStatistics();
    }

    @Override
    public void accept(long value) {
        currentBucket().accept(value);
    }

    public void accept(long... value) {
        LongSummaryStatistics currentBucket = currentBucket();
        Arrays.stream(value).forEach(currentBucket);
    }

    @Override
    public LongSummaryStatistics getWindowValue() {
        LongSummaryStatistics result = new LongSummaryStatistics();
        LongSummaryStatistics[] b = getBuckets();
        for (int i = b.length -1 ; i >= 0; i--) {
            result.combine(b[i]);

        }
        return result;
    }

    @Deprecated
    public LongSummaryStatistics getCombined() {
        return getWindowValue();
    }

}
