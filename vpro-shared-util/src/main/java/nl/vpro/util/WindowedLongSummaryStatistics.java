package nl.vpro.util;

import lombok.Builder;

import java.time.Duration;
import java.time.Instant;
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
    LongSummaryStatistics[] newBuckets(int bucketCount) {
        return new LongSummaryStatistics[bucketCount];

    }

    @Override
    LongSummaryStatistics initialValue() {
        return new LongSummaryStatistics();
    }

    public void accept(Long value) {
        currentBucket().accept(value);
    }

    public LongSummaryStatistics getCombined() {
        Instant now = Instant.now();
        LongSummaryStatistics result = new LongSummaryStatistics();
        LongSummaryStatistics[] b = getBuckets();
        for (int i = 0 ; i < b.length ; i++) {
            result.combine(b[i]);
            if (now.isBefore(start.plusMillis((i + 1) * bucketDuration))) {
                break;
            }
        }
        return result;
    }

}
