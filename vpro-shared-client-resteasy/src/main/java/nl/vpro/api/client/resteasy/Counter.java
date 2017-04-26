package nl.vpro.api.client.resteasy;

import lombok.Builder;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import javax.management.MXBean;
import javax.management.ObjectName;

import nl.vpro.util.WindowedEventRate;
import nl.vpro.util.WindowedLongSummaryStatistics;

import static nl.vpro.util.TimeUtils.roundToMillis;

/**
 * @author Michiel Meeuwissen
 * @since 1.57
 */
@MXBean
@Slf4j
@ToString
public class Counter implements CounterMXBean {


    private final AtomicLong count = new AtomicLong(0L);
    private final WindowedEventRate rate;
    private final WindowedLongSummaryStatistics durations;
    private final ObjectName name;

    @Builder
    protected Counter(
        ObjectName name,
        Duration countWindow,
        Integer bucketCount
        ) {
        this.name = name;
        rate = WindowedEventRate.builder()
            .window(countWindow)
            .bucketCount(bucketCount)
            .build();
        durations = WindowedLongSummaryStatistics.builder()
            .window(countWindow)
            .bucketCount(bucketCount)
            .build();
        if (name != null) {
            AbstractApiClient.registerBean(name, this);
        }
    }


    @Override
    public long getCount() {
        return count.get();

    }

    @Override
    public double getRate() {
        return rate.getRate(TimeUnit.MINUTES);
    }


    @Override
    public String getRateWindow() {
        return rate.getTotalDuration().toString();
    }

    @Override
    public String getAverageDuration() {
        return
            Duration.ofMillis((long) (durations.getCombined().getAverage())).toString();
            // No standard deviation (introduces commons-math for that?)
    }
    @Override
    public Map<String, String> getAverageDurations()  {
        return durations.getRanges()
            .entrySet()
            .stream()
            .collect(Collectors.toMap(
                e -> e.getKey().toString(),
                e -> Duration.ofMillis((long) (e.getValue().getAverage())).toString()
            ));
    }

    public WindowedLongSummaryStatistics getDurations() {
        return durations;
    }

    private void increment() {
        rate.newEvent();
        count.getAndIncrement();
    }

    void eventAndDuration(Duration duration) {
        increment();
        log.debug("{} Duration {}", this, duration);
        durations.accept(roundToMillis(duration).toMillis());
    }

    void shutdown() {
        if (name != null) {
            AbstractApiClient.unregister(name);
        }
    }

}
