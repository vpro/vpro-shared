package nl.vpro.api.client.resteasy;

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


    @Description("Current event rate averaged over last 30 minutes")
    @Units("events/minute")
    double getRate();
}
