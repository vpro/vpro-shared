package nl.vpro.javax.cache.managment;

import nl.vpro.jmx.Description;

public interface CacheWrapperMXBean {


    @Description("Clears cache")
    String removeAll();

}
