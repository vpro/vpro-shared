package nl.vpro.javax.cache.managment;

import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.inject.Inject;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import nl.vpro.jmx.MBeans;


/**
 * This registers MBeans for every cache in a {@link CacheManager}.
 */
@Slf4j
public class CacheManagerMXBeans {


    final CacheManager cacheManager;

    @Inject
    CacheManagerMXBeans(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }
    @PostConstruct
    public void init() {
        this.cacheManager.getCacheNames().forEach(c -> {
            try {
                Cache<?, ?> cache = this.cacheManager.getCache(c);
                ObjectName name = new ObjectName(
                    String.format("javax.cache:type=Cache,CacheManager=%s,Cache=%s",
                        sanitize(cacheManager.getURI().toString()), sanitize(cache.getName())));
                MBeans.registerBean(name, new CacheWrapper(cache));
            } catch (MalformedObjectNameException e) {
                log.warn(e.getMessage(), e);
            }
        });
        try {
            ObjectName name = new ObjectName(
                String.format("javax.cache:type=CacheManager,CacheManager=%s",
                    sanitize(cacheManager.getURI().toString())));
            MBeans.registerBean(name, new CacheManagerWrapper(cacheManager));
        } catch (MalformedObjectNameException e) {
            log.warn(e.getMessage(), e);
        }
    }

    // copied from ehcache code, so have exact same escaping
    private String sanitize(String string) {
        return string == null ? "" : string.replaceAll("[,:=\n]", ".");
    }
}
