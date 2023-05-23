package nl.vpro.monitoring.config;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.binder.cache.JCacheMetrics;
import io.micrometer.core.instrument.binder.db.PostgreSQLDatabaseMetrics;
import io.micrometer.core.instrument.binder.jvm.*;
import io.micrometer.core.instrument.binder.logging.Log4j2Metrics;
import io.micrometer.core.instrument.binder.system.DiskSpaceMetrics;
import io.micrometer.core.instrument.binder.system.*;
import io.micrometer.core.instrument.binder.tomcat.TomcatMetrics;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.*;

import javax.annotation.PreDestroy;
import javax.cache.CacheManager;
import javax.sql.DataSource;

import org.apache.camel.CamelContext;
import org.apache.camel.component.micrometer.routepolicy.MicrometerRoutePolicyFactory;
import org.apache.catalina.Manager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.HibernateMetrics;
import org.hibernate.stat.HibernateQueryMetrics;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import nl.vpro.util.locker.ObjectLocker;
import nl.vpro.util.locker.ObjectLockerAdmin;

import static nl.vpro.util.locker.ObjectLockerAdmin.JMX_INSTANCE;

@Configuration
@Slf4j
public class MonitoringConfig {

    public static PrometheusMeterRegistry meterRegistry;

    @Autowired(required = false)
    public MonitoringProperties properties = new MonitoringProperties();

    @Autowired
    private ApplicationContext applicationContext;

    private final List<AutoCloseable> closables = new ArrayList<>();

    @Bean("globalMeterRegistry")
    public PrometheusMeterRegistry globalMeterRegistry() {
        if (meterRegistry == null) {
            final PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
            meterRegistry = registry;
        }
        return meterRegistry;
    }

    @PreDestroy
    public void shutdown() {
        for (AutoCloseable c : closables) {
            try {
                c.close();
                log.info("Closed {}", c);
            } catch (Exception e) {
                log.warn("{}: {}", c, e.getMessage());
            }
        }
    }

    private boolean started = false;
    @EventListener
    public synchronized void init(ContextRefreshedEvent event) throws BeansException {
        if (! started) {
            start(globalMeterRegistry());
        }
        started = true;
    }


    @SuppressWarnings("unchecked")
    protected void start(MeterRegistry registry) {
        if (properties.getCommonTags() != null) {
            registry.config().commonTags(properties.getCommonTags().toArray(new String[0]));
        }
        if (properties.isMeterClassloader()) {
            new ClassLoaderMetrics().bindTo(registry);
        }

        if (classForName("org.apache.logging.log4j.core.config.Configuration").isPresent()) {
            Log4j2Metrics metrics = new Log4j2Metrics();
            metrics.bindTo(registry);
            closables.add(metrics);
        }

        final Optional<?> cacheManager = getCacheManager();
        if (properties.isMeterJCache() && cacheManager.isPresent()) {
            CacheManager manager = (CacheManager)  cacheManager.get();
            manager.getCacheNames().forEach(cacheName -> {
                new JCacheMetrics<>(manager.getCache(cacheName), Tags.empty()).bindTo(registry);
            });
        }
        if (properties.isMeterJvmHeap()) {
            JvmHeapPressureMetrics metrics = new JvmHeapPressureMetrics();
            metrics.bindTo(registry);
            closables.add(metrics);
        }
        if (properties.isMeterJvmGc()) {
            JvmGcMetrics metrics = new JvmGcMetrics();
            metrics.bindTo(registry);
            closables.add(metrics);
        }
        if (properties.isMeterJvmMemory()) {
            new JvmMemoryMetrics().bindTo(registry);
        }
        if (properties.isMeterJvmThread()) {
            new JvmThreadMetrics().bindTo(registry);
        }

        if (classForName("org.hibernate.SessionFactory").isPresent()) {

            try {
                if (properties.isMeterHibernate()) {
                    final Optional<SessionFactory> sessionFactory = (Optional<SessionFactory>) getSessionFactory();
                    if (sessionFactory.isPresent()) {
                        new HibernateMetrics(
                            sessionFactory.get(),
                            properties.getMeterHibernateName(),
                            Tags.empty()
                        ).bindTo(registry);

                    } else {
                        log.warn("No session factory to monitor (hibernate)");
                    }
                }
            } catch (java.lang.NoClassDefFoundError noClassDefFoundError) {
                log.warn("No hibernate metrics: Missing class {}", noClassDefFoundError.getMessage());
            }
            try {
                if (properties.isMeterHibernateQuery()) {
                    final Optional<SessionFactory> sessionFactory = (Optional<SessionFactory>) getSessionFactory();

                    if (sessionFactory.isPresent()) {
                        new HibernateQueryMetrics(sessionFactory.get(), properties.getMeterHibernateName(), Tags.empty()).bindTo(registry);
                    } else {
                        log.warn("No session factory to monitor (hibernate query)");
                    }
                }
            } catch (java.lang.NoClassDefFoundError noClassDefFoundError) {
                log.warn("No hibernate query metrics. Missing class {}", noClassDefFoundError.getMessage());
            }
        } else {
            log.debug("No org.hibernate.SessionFactory");
        }
        try {
            if (properties.isMeterPostgres()) {
                final Optional<DataSource> dataSource = (Optional<DataSource>) getDataSource();
                if (dataSource.isPresent()) {
                    if (properties.getPostgresDatabaseName() != null) {
                        new PostgreSQLDatabaseMetrics(dataSource.get(), properties.getPostgresDatabaseName()).bindTo(registry);
                    } else {
                        log.error("For metering postgres one should provide an existing database name");
                    }
                }
            }
        } catch (java.lang.NoClassDefFoundError noClassDefFoundError) {
            log.warn("No hibernate postgresql metrics. Missing class {}", noClassDefFoundError.getMessage());
        }
        if (properties.isMeterProcessor()) {
            new ProcessorMetrics().bindTo(registry);
        }

        if (classForName("org.apache.camel.component.micrometer.routepolicy.MicrometerRoutePolicyFactory").isPresent()) {
            try {
                MicrometerRoutePolicyFactory factory = new MicrometerRoutePolicyFactory();
                factory.setMeterRegistry(meterRegistry);
                CamelContext camelContext = applicationContext.getBean(CamelContext.class);
                camelContext.addRoutePolicyFactory(factory);
                log.info("Set up {}", factory);
            } catch (Exception e) {
                log.warn(e.getMessage(), e);
            }
        }


        final Optional<Manager> manager = (Optional<Manager>) getManager();
        if (properties.isMeterTomcat() && manager.isPresent()) {
            TomcatMetrics metrics = new TomcatMetrics(manager.get(), Tags.empty());
            metrics.bindTo(registry);
            closables.add(metrics);
        }
        if (properties.isMeterUptime()) {
            new UptimeMetrics().bindTo(registry);
        }
        if (properties.getMeterVolumes() != null) {
            for (String folder : properties.getMeterVolumes()) {
                File dir = new File(folder);
                if (dir.exists()) {
                    new DiskSpaceMetrics(dir).bindTo(registry);
                } else {
                    log.info("Not metring {}, because it does not exist", dir);
                }
            }
        }
        if (properties.isMeterLocks()) {
            ObjectLocker.listen((type, holder, duration) -> {
                Object key = holder.key;
                String keyType = key instanceof ObjectLocker.DefinesType ? String.valueOf(((ObjectLocker.DefinesType) key).getType()) : key.getClass().getSimpleName();
                registry.counter("locks.event", "keyType", keyType, "eventType", String.valueOf(type), "holdCount", String.valueOf(holder.lock.getHoldCount())).increment();
                if (type == ObjectLocker.Listener.Type.UNLOCK) {
                    registry.timer("locks.duration", "keyType", keyType).record(duration);
                }
            });
            Gauge.builder("locks.count", JMX_INSTANCE, ObjectLockerAdmin::getCurrentCount)
                .description("The current number of locked objects")
                .register(registry);

            Gauge.builder("locks.total_count", JMX_INSTANCE, ObjectLockerAdmin::getLockCount)
                .description("The total number of locked objects until now")
                .register(registry);

            Gauge.builder("locks.average_acquiretime",
                    () -> JMX_INSTANCE.getAverageLockAcquireTime().getWindowValue().optionalDoubleMean().orElse(0d)
                )
                .description("The average time in ms to acquire a lock (in " + JMX_INSTANCE.getAverageLockAcquireTime().getTotalDuration() + ")")
                .register(registry);

            Gauge.builder("locks.average_duration",
                    () -> JMX_INSTANCE.getAverageLockDuration().getWindowValue().optionalDoubleMean().orElse(0d)
                )
                .description("The average time in ms to hold a lock (in " + JMX_INSTANCE.getAverageLockDuration().getTotalDuration() +")")
                .register(registry);


            Gauge.builder("locks.max_concurrency", JMX_INSTANCE, ObjectLockerAdmin::getMaxConcurrency)
                .description("The maximum number threads waiting for the same object")
                .register(registry);

            Gauge.builder("locks.current_max_concurrency", () -> ObjectLocker.getLockedObjects().values().stream().mapToInt(l -> l.lock.getHoldCount()).max().orElse(0))
                .description("The maximum number threads waiting for the same object")
                .register(registry);

            Gauge.builder("locks.maxDepth", JMX_INSTANCE, ObjectLockerAdmin::getMaxConcurrency)
                .description("The maximum number of locked objects in the same thread")
                .register(registry);

        }
    }

    // With a datasource argument instead of the generic wildcard, application startup
    // will fail when this DataSource class is not available
    private Optional<?> getDataSource() {
        return classForName("javax.sql.DataSource")
            .flatMap(this::getBean);
    }

    private Optional<?> getSessionFactory() {
        return classForName("org.hibernate.SessionFactory")
            .flatMap(this::getBean);
    }

    private Optional<?> getCacheManager() {
        return classForName("javax.cache.CacheManager")
            .flatMap(this::getBean);
    }

    private Optional<?> getManager() {
        return classForName("org.apache.catalina.Manager")
            .flatMap(this::getBean);
    }

    private Optional<Class<?>> classForName(String name) {
        try {
            final Class<?> aClass = Class.forName(name, true, this.getClass().getClassLoader());
            return Optional.of(aClass);
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }
    }

    private Optional<?> getBean(Class<?> clazz) {
        return Optional.ofNullable(applicationContext)
            .map(ac -> {
                try {
                    return ac.getBean(clazz);
                } catch (BeansException e) {
                    return null;
                }
            });
    }
}
