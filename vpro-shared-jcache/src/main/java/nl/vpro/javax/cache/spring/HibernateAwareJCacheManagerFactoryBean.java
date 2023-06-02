package nl.vpro.javax.cache.spring;

import javax.cache.Caching;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.cache.jcache.JCacheManagerFactoryBean;

public class HibernateAwareJCacheManagerFactoryBean  extends JCacheManagerFactoryBean {

     {
                // See, MSE-5473 This is what org.hibernate.cache.jcache.internal.JCacheRegionFactory ends up doing.
         super.setBeanClassLoader(Caching.getCachingProvider().getDefaultClassLoader());

     }
    @Override
    public void setBeanClassLoader(@NonNull ClassLoader classLoader) {
        // just disable this. Will otherwise be called by spring.
        // But it must remain the class loader that also hibernate, otherwise
        // we'll end up with to cache managers.
    }
}
