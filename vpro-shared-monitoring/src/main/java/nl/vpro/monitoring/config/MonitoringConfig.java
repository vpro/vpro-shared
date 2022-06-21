package nl.vpro.monitoring.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.cache.EhCache2Metrics;
import io.micrometer.core.instrument.binder.db.PostgreSQLDatabaseMetrics;
import io.micrometer.core.instrument.binder.jvm.*;
import io.micrometer.core.instrument.binder.system.DiskSpaceMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.core.instrument.binder.tomcat.TomcatMetrics;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import lombok.extern.slf4j.Slf4j;
import net.sf.ehcache.Ehcache;

import java.io.File;
import java.util.Optional;

import javax.sql.DataSource;

import org.apache.catalina.Manager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.HibernateMetrics;
import org.hibernate.stat.HibernateQueryMetrics;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import nl.vpro.util.locker.ObjectLocker;
import nl.vpro.util.locker.ObjectLockerAdmin;

import static nl.vpro.util.locker.ObjectLocker.Listener.Type.LOCK;

@Configuration
@Slf4j
public class MonitoringConfig {

    @Autowired(required = false)
    public MonitoringProperties properties = new MonitoringProperties();

    @Autowired
    private ApplicationContext applicationContext;

    @Bean("globalMeterRegistry")
    public PrometheusMeterRegistry globalMeterRegistry() {
        final PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        start(registry);
        return registry;
    }

    @SuppressWarnings("unchecked")
    protected void start(PrometheusMeterRegistry registry) {
        if (properties.getCommonTags() != null) {
            registry.config().commonTags(properties.getCommonTags().toArray(new String[0]));
        }
        if (properties.isMeterClassloader()) {
            new ClassLoaderMetrics().bindTo(registry);
        }

        if (classForName("org.apache.logging.log4j.core.config.Configuration").isPresent()) {
            new io.micrometer.core.instrument.binder.logging.Log4j2Metrics().bindTo(registry);
        }

        final Optional<Ehcache> ehCache = (Optional<Ehcache>) getEhCache();
        if (properties.isMeterEhCache2() && ehCache.isPresent()) {
            new EhCache2Metrics(ehCache.get(), Tags.empty()).bindTo(registry);
        }
        if (properties.isMeterJvmHeap()) {
            new JvmHeapPressureMetrics().bindTo(registry);
        }
        if (properties.isMeterJvmGc()) {
            new JvmGcMetrics().bindTo(registry);
        }
        if (properties.isMeterJvmMemory()) {
            new JvmMemoryMetrics().bindTo(registry);
        }
        if (properties.isMeterJvmThread()) {
            new JvmThreadMetrics().bindTo(registry);
        }
        try {
            if (properties.isMeterHibernate()) {

                final Optional<SessionFactory> sessionFactory = (Optional<SessionFactory>) getSessionFactory();
                if (sessionFactory.isPresent()) {
                    new HibernateMetrics(sessionFactory.get(), properties.getMeterHibernateName(), Tags.empty()).bindTo(registry);

                } else {
                    log.warn("No session factory to monitor");
                }
            }
        } catch(java.lang.NoClassDefFoundError noClassDefFoundError) {
            log.warn("No hibernate metrics: Missing class {}", noClassDefFoundError.getMessage());
        }
        try {
            if (properties.isMeterHibernateQuery()) {
                final Optional<SessionFactory> sessionFactory = (Optional<SessionFactory>) getSessionFactory();

                if (sessionFactory.isPresent()) {
                    new HibernateQueryMetrics(sessionFactory.get(), properties.getMeterHibernateName(), Tags.empty()).bindTo(registry);
                } else {
                    log.warn("No session factory to monitor");
                }
            }
        } catch (java.lang.NoClassDefFoundError noClassDefFoundError) {
            log.warn("No hibernate query metrics. Missing class {}", noClassDefFoundError.getMessage());
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
        final Optional<Manager> manager = (Optional<Manager>) getManager();
        if (properties.isMeterTomcat() && manager.isPresent()) {
            new TomcatMetrics(manager.get(), Tags.empty()).bindTo(registry);
        }
        if (properties.isMeterUptime()) {
            new UptimeMetrics().bindTo(registry);
        }
        if (properties.getMeterVolumes() != null) {
            for (String folder : properties.getMeterVolumes()) {
                new DiskSpaceMetrics(new File(folder)).bindTo(registry);
            }
        }
        if (properties.isMeterLocks()) {
            ObjectLocker.listen((type, holder, duration) -> {
                if (holder.lock.getHoldCount() == 1 && type == LOCK) {
                    Object key = holder.key;
                    String keyType = key instanceof ObjectLocker.DefinesType ? String.valueOf(((ObjectLocker.DefinesType) key).getType()) : key.getClass().getSimpleName();
                    registry.counter("locks.event", "type", keyType).increment();
                }
            });
            Gauge.builder("locks.count", ObjectLockerAdmin.JMX_INSTANCE, ObjectLockerAdmin::getCurrentCount)
                .description("The current number of locked objects")
                .register(registry);

            Gauge.builder("locks.total_count", ObjectLockerAdmin.JMX_INSTANCE, ObjectLockerAdmin::getLockCount)
                .description("The total number of locked objects until now")
                .register(registry);

            Gauge.builder("locks.average_acquiretime", () -> ObjectLockerAdmin.JMX_INSTANCE.getAverageLockAcquireTime().getWindowValue().getValue())
                .description("The average time in ms to acquire a lock")
                .register(registry);

            Gauge.builder("locks.max_concurrency", ObjectLockerAdmin.JMX_INSTANCE, ObjectLockerAdmin::getMaxConcurrency)
                .description("The maximum number threads waiting for the same object")
                .register(registry);

            Gauge.builder("locks.current_max_concurrency", () -> ObjectLocker.getLockedObjects().values().stream().mapToInt(l -> l.lock.getHoldCount()).max().orElse(0))
                .description("The maximum number threads waiting for the same object")
                .register(registry);

            Gauge.builder("locks.maxDepth", ObjectLockerAdmin.JMX_INSTANCE, ObjectLockerAdmin::getMaxConcurrency)
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

    private Optional<?> getEhCache() {
        return classForName("net.sf.ehcache.Ehcache")
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
