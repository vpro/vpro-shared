package nl.vpro.api.client.resteasy;

import java.time.Duration;

import javax.management.MXBean;

import nl.vpro.jmx.Description;
import nl.vpro.jmx.Units;

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


    @Description("Window of the getRate call")
    @Units("a duration")
    String getRateWindow();
}
