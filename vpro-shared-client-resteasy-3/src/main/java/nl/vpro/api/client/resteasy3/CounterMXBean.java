package nl.vpro.api.client.resteasy3;

import nl.vpro.jmx.Description;
import nl.vpro.jmx.Units;

import javax.management.MXBean;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Michiel Meeuwissen
 * @since 1.57
 */
@MXBean
public interface CounterMXBean {

    @Description("Total call")
    @Units("number")
    long getCount();


    @Description("Current event rate averaged over a period")
    @Units("events/minute")
    double getRate();


    @Description("Window of the getRate calls")
    @Units("a duration")
    String getRateWindow();

    @Description("Gets the average duration per bucket")
    Map<String, String> getAverageDurations();

    @Description("Gets the average duration in ms per bucket")
    @Units("ms")
    default Map<String, Long> getAverageDurationsMs() {
        return getAverageDurations().entrySet()
            .stream().collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    e -> Duration.parse(e.getValue()).toMillis()
                ));
    }

    @Description("Gets the average duration during the past window")
    String getAverageDuration();


    @Description("Gets the average duration duration the past window in ms")
    default long getAverageDurationMs() {
        return Duration.parse(getAverageDuration()).toMillis();
    }




}
