package nl.vpro.api.client.resteasy;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import javax.management.MXBean;
import javax.management.ObjectName;

import nl.vpro.util.WindowedEventRate;
import nl.vpro.util.WindowedLongSummaryStatistics;

/**
 * @author Michiel Meeuwissen
 * @since 1.57
 */
@MXBean
public class Counter implements CounterMXBean {


    private final AtomicLong count = new AtomicLong(0L);
    private final WindowedEventRate rate;
    private final WindowedLongSummaryStatistics durations;
    private final ObjectName name;

    public Counter(
        ObjectName name,
        Duration countWindow) {
        this.name = name;
        rate = WindowedEventRate.builder()
            .window(countWindow)
            .build();
        durations = WindowedLongSummaryStatistics.builder()
            .window(countWindow)
            .build();
        AbstractApiClient.registerBean(name, this);
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
            Duration.ofMillis((long) (durations.getCombined().getAverage() * 1000L)).toString()
            // No standard deviation (introduces commons-math for that?)
            ;
    }
    @Override
    public Map<String, String> getAverageDurations()  {
        return durations.getRanges()
            .entrySet()
            .stream()
            .collect(Collectors.toMap(
                e -> e.getKey().toString(),
                e -> Duration.ofMillis((long) (e.getValue().getAverage() * 1000L)).toString()
            ));
    }

    public WindowedLongSummaryStatistics getDurations() {
        return durations;
    }

    protected void incrementAndGet() {
        rate.newEvent();
        count.incrementAndGet();
    }

    void eventAndDuration(Duration duration) {
        incrementAndGet();
        durations.accept(duration.toMillis());
    }

    void shutdown() {
        AbstractApiClient.unregister(name);
    }

}
