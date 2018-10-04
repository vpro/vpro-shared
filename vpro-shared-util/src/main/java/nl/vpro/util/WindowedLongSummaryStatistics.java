package nl.vpro.util;

import lombok.Builder;

import java.time.Duration;
import java.util.Arrays;
import java.util.LongSummaryStatistics;

/**
 * @author Michiel Meeuwissen
 * @since 1.66
 */
public class WindowedLongSummaryStatistics extends Windowed<LongSummaryStatistics> {

    @Builder
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

    public void accept(long... value) {
        LongSummaryStatistics currentBucket = currentBucket();
        Arrays.stream(value).forEach(currentBucket);
    }

    public LongSummaryStatistics getCombined() {
        LongSummaryStatistics result = new LongSummaryStatistics();
        LongSummaryStatistics[] b = getBuckets();
        for (int i = b.length -1 ; i >= 0; i--) {
            result.combine(b[i]);

        }
        return result;
    }

    @Override
    public String toString() {
        return super.toString() + ":" + getCombined().toString();
    }

}
