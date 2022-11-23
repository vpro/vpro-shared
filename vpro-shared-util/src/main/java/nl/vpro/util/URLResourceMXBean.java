package nl.vpro.util;

import nl.vpro.jmx.Description;

public interface URLResourceMXBean {

    @Description("Expires the cache")
    void expire();
}
