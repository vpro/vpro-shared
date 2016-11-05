package nl.vpro.api.client.resteasy;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.MXBean;
import javax.management.ObjectName;

import nl.vpro.util.WindowedEventRate;

/**
 * @author Michiel Meeuwissen
 * @since 1.57
 */
@MXBean
public class Counter implements CounterMXBean{


    private final AtomicLong count = new AtomicLong(0L);
    private final WindowedEventRate rate = WindowedEventRate.builder()
        .windowCount(30)
        .windowSize(Duration.ofMinutes(1))
        .build();

    public Counter(ObjectName name) {
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

    public long incrementAndGet() {
        rate.newEvent();
        return count.incrementAndGet();
    }
}
