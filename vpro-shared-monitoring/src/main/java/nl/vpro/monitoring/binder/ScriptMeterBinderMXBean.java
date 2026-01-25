package nl.vpro.monitoring.binder;

import nl.vpro.jmx.Description;

public interface ScriptMeterBinderMXBean {

    @Description("run")
    void run();

    @Description("duration")
    String getDuration();

    void setDuration(String duration);

    @Description("interval")
    String getScheduleInterval();

    void setScheduleInterval(String duration);
}
