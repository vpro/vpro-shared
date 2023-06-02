package nl.vpro.javax.cache.managment;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.cache.Cache;
import javax.cache.CacheManager;

public class CacheManagerWrapper implements CacheManagerWrapperMXBean {


    final CacheManager cacheManager;

    public CacheManagerWrapper(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public String clearAll() {
        List<String> cleared = new ArrayList<>();
        for (String cacheName : cacheManager.getCacheNames()) {
            Cache<?, ?> cache = cacheManager.getCache(cacheName);
            cache.removeAll();
            cleared.add(cacheName);
        }
        return "Cleared " + cleared.stream().collect(Collectors.joining(", "));
    }
}
