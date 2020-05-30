package nl.vpro.util;

import java.time.Duration;
import java.util.*;

/**
 * {@link DoubleSummaryStatistics} can be aggregated, and there for {@link Windowed}.
 * @author Michiel Meeuwissen
 * @since 2.2
 */
public class WindowedDoubleSummaryStatistics extends Windowed<DoubleSummaryStatistics> {

    @lombok.Builder(builderClassName = "Builder")
    protected WindowedDoubleSummaryStatistics(
        Duration window,
        Duration bucketDuration,
        Integer bucketCount) {
        super(window, bucketDuration, bucketCount);
    }

    @Override
    protected DoubleSummaryStatistics[] newBuckets(int bucketCount) {
        return new DoubleSummaryStatistics[bucketCount];

    }

    @Override
    protected DoubleSummaryStatistics initialValue() {
        return new DoubleSummaryStatistics();
    }

    public void accept(double... value) {
        DoubleSummaryStatistics currentBucket = currentBucket();
        Arrays.stream(value).forEach(currentBucket);
    }

    public DoubleSummaryStatistics getCombined() {
        DoubleSummaryStatistics result = new DoubleSummaryStatistics();
        DoubleSummaryStatistics[] b = getBuckets();
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
