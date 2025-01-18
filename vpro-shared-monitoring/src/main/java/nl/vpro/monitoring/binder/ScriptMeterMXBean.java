package nl.vpro.monitoring.binder;

import nl.vpro.jmx.Description;

public interface ScriptMeterMXBean {

    @Description("run")
    void run();

    @Description("duration")
    String getDuration();

    void setDuration(String duration);
}
