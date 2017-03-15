package nl.vpro.api.client.resteasy;

import java.time.Duration;

import org.assertj.core.data.Percentage;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 1.66
 */
public class CounterTest {


    @Test
    public void test() throws InterruptedException {
        Counter counter = Counter.builder()
            .bucketCount(10)
            .countWindow(Duration.ofSeconds(10))
            .build();

        counter.eventAndDuration(Duration.ofMillis(500));
        Thread.sleep(500);

        // 1 event in .5 seconds is about 120 /min.
        assertThat(counter.getRate()).isCloseTo(120, Percentage.withPercentage(20));

        assertThat(counter.getAverageDuration()).isEqualTo("PT0.5S");


    }

}
