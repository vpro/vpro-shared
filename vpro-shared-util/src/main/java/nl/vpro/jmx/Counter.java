package nl.vpro.jmx;

import lombok.Builder;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.*;
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
@ToString(exclude = "name")
public class Counter implements CounterMXBean {


    private final AtomicLong count = new AtomicLong(0L);
    private final WindowedEventRate rate;
    private final List<WindowedLongSummaryStatistics> durationStatistics = new ArrayList<>();
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
        durationStatistics.add(WindowedLongSummaryStatistics.builder()
            .window(countWindow)
            .bucketCount(bucketCount)
            .build());
        if (name != null) {
            MBeans.registerBean(name, this);
        }
    }

    private WindowedLongSummaryStatistics newStatistics() {
        WindowedLongSummaryStatistics template = durationStatistics.get(0);
        return WindowedLongSummaryStatistics.builder()
            .window(template.getTotalDuration())
            .bucketCount(template.getBucketCount())
            .build();
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
            Duration.ofMillis((long) (getDurationStatistics().getCombined().getAverage())).toString();
            // No standard deviation (introduces commons-math for that?)
    }
    @Override
    public Map<String, String> getAverageDurations()  {
        return getDurationStatistics().getRanges()
            .entrySet()
            .stream()
            .collect(Collectors.toMap(
                e -> e.getKey().toString(),
                e -> Duration.ofMillis((long) (e.getValue().getAverage())).toString()
            ));
    }

    public WindowedLongSummaryStatistics getDurationStatistics() {
        return getDurationStatistics(0);
    }

    public WindowedLongSummaryStatistics getDurationStatistics(int index) {
        return durationStatistics.get(index);
    }
    private void increment() {
        rate.newEvent();
        count.getAndIncrement();
    }

    void eventAndDuration(Duration duration, Duration... durations) {
        increment();
        log.debug("{} Duration {}", this, duration);
        getDurationStatistics().accept(roundToMillis(duration).toMillis());
        int i = 1;
        for(Duration dur : durations) {
            if (this.durationStatistics.size() <= i) {
                this.durationStatistics.add(newStatistics());
            }
            this.durationStatistics.get(i++).accept(roundToMillis(dur).toMillis());
        }
    }

    public void shutdown() {
        if (name != null) {
            MBeans.unregister(name);
        }
    }

}
