package nl.vpro.javax.cache.managment;

import javax.cache.Cache;

public class CacheWrapper implements CacheWrapperMXBean {


    final Cache<?, ?> cache;

    public CacheWrapper(Cache<?, ?> cache) {
        this.cache = cache;
    }

    @Override
    public String removeAll() {
        cache.removeAll();
        return "Cleared " + cache;
    }
}
