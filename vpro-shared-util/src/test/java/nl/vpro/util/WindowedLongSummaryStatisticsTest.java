package nl.vpro.util;

import java.time.Duration;
import java.util.LongSummaryStatistics;

import org.assertj.core.data.Offset;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 1.66
 */
public class WindowedLongSummaryStatisticsTest {

    @Test
    public void test() throws InterruptedException {
        WindowedLongSummaryStatistics instance =
            WindowedLongSummaryStatistics.builder()
                .bucketCount(10)
                .bucketDuration(Duration.ofSeconds(1))
                .build();

        instance.accept(100L);
        instance.accept(200L);
        Thread.sleep(1000L);
        instance.accept(200L);
        instance.accept(300L);
        LongSummaryStatistics[] bucket = instance.getBuckets();
        assertThat(instance.getBuckets()[0].getAverage()).isCloseTo(250, Offset.offset(0.1));
        assertThat(instance.getBuckets()[1].getAverage()).isCloseTo(150, Offset.offset(0.1));

        assertThat(instance.getCombined().getAverage()).isCloseTo(200, Offset.offset(0.1));
        System.out.println(instance.getRanges());
    }

}
