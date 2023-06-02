package nl.vpro.javax.cache.managment;

import nl.vpro.jmx.Description;

public interface CacheManagerWrapperMXBean {


    @Description("Clears all caches")
    String clearAll();

}
