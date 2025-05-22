package nl.vpro.monitoring.config;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.binder.cache.JCacheMetrics;
import io.micrometer.core.instrument.binder.db.PostgreSQLDatabaseMetrics;
import io.micrometer.core.instrument.binder.jvm.*;
import io.micrometer.core.instrument.binder.logging.Log4j2Metrics;
import io.micrometer.core.instrument.binder.system.DiskSpaceMetrics;
import io.micrometer.core.instrument.binder.system.*;
import io.micrometer.core.instrument.binder.tomcat.TomcatMetrics;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import jakarta.annotation.PreDestroy;

import org.apache.catalina.Manager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.HibernateMetrics;
import org.hibernate.stat.HibernateQueryMetrics;
import org.slf4j.event.Level;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jmx.export.annotation.ManagedOperation;


import nl.vpro.monitoring.binder.JvmMaxDirectMemorySize;
import nl.vpro.monitoring.binder.ScriptMeterBinder;
import nl.vpro.util.locker.ObjectLocker;
import nl.vpro.util.locker.ObjectLockerAdmin;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.sql.DataSource;

import static io.micrometer.core.instrument.Gauge.builder;
import static nl.vpro.util.locker.ObjectLockerAdmin.JMX_INSTANCE;
import static org.slf4j.event.Level.DEBUG;
import static org.slf4j.event.Level.WARN;

/**
 * Sets up Prometheus {@link MeterRegistry}.
 */
@Configuration
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MeterRegistryConfiguration {

    public static PrometheusMeterRegistry meterRegistry;


    MonitoringProperties monitoringProperties = new MonitoringProperties();


    @Autowired
    private ApplicationContext applicationContext;

    private final List<AutoCloseable> closables = new ArrayList<>();

    @Getter
    private final List<String> warnings = new ArrayList<>();

    @Bean("globalMeterRegistry")
    public PrometheusMeterRegistry createMeterRegistry() {
        final PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        if (meterRegistry != null) {
            log.info("Replacing meterRegistry {} -> {}", meterRegistry, registry);
        }
        meterRegistry = registry;
        return meterRegistry;
    }
    @Bean
    public MonitoringProperties monitoringProperties() {
        return monitoringProperties;
    }


    public PrometheusMeterRegistry getGlobalMeterRegistry() {
        if (meterRegistry == null) {
            createMeterRegistry();
        }
        init(null);
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
        if (!started) {
            if (meterRegistry == null) {
                createMeterRegistry();
            }
            start(meterRegistry);
        }
        started = true;
    }


    protected void start(MeterRegistry registry) {
        configure(registry);
    }

    @ManagedOperation
    public PrometheusMeterRegistry configure() {
        configure(meterRegistry);
        return meterRegistry;
    }

    @SuppressWarnings("unchecked")
    protected void configure(MeterRegistry registry) {
        if (monitoringProperties.getCommonTags() != null) {
            registry.config().commonTags(monitoringProperties.getCommonTags().toArray(new String[0]));
        }
        if (monitoringProperties.isMeterClassloader()) {
            new ClassLoaderMetrics().bindTo(registry);
        }

        if (monitoringProperties.isMeterLog4j()) {
            if (classForName("org.apache.logging.log4j.core.config.Configuration").isPresent()) {

                Log4j2Metrics metrics = new Log4j2Metrics();
                metrics.bindTo(registry);
                closables.add(metrics);
            }
        }


        final Optional<?> cacheManager = getCacheManager();
        if (monitoringProperties.isMeterJCache() && cacheManager.isPresent()) {
            try {
                Object manager = cacheManager.get();
                Method m = manager.getClass().getMethod("getCacheNames");
                Method getCache = manager.getClass().getMethod("getCache", String.class);

                ((List) m.invoke(manager)).forEach(cacheName -> {
                    try {
                        Object cache = getCache.invoke(manager, cacheName);
                        new JCacheMetrics<>((javax.cache.Cache) cache, Tags.empty()).bindTo(registry);
                    } catch (InvocationTargetException | IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        if (monitoringProperties.isMeterJvmHeap()) {
            JvmHeapPressureMetrics metrics = new JvmHeapPressureMetrics();
            metrics.bindTo(registry);
            closables.add(metrics);
        }
        if (monitoringProperties.isMeterJvmGc()) {
            JvmGcMetrics metrics = new JvmGcMetrics();
            metrics.bindTo(registry);
            closables.add(metrics);
        }
        if (monitoringProperties.isMeterJvmMemory()) {
            new JvmMemoryMetrics().bindTo(registry);
            new JvmMaxDirectMemorySize().bindTo(registry);
        }
        if (monitoringProperties.isMeterJvmThread()) {
            new JvmThreadMetrics().bindTo(registry);
        }


        if (monitoringProperties.isMeterHibernate() && classForName("org.hibernate.SessionFactory").isPresent() && classForName("org.hibernate.stat.Statistics").isPresent() && classForName("org.hibernate.stat.HibernateMetrics").isPresent()) {
            final Optional<SessionFactory> sessionFactory = (Optional<SessionFactory>) getSessionFactory();
            if (sessionFactory.isPresent()) {
                new HibernateMetrics(
                    sessionFactory.get(),
                    monitoringProperties.getMeterHibernateName(),
                    Tags.empty()
                ).bindTo(registry);

            } else {
                warn("No session factory to monitor (hibernate)");
            }
        }
        if (monitoringProperties.isMeterHibernateQuery() && classForName("org.hibernate.SessionFactory").isPresent()) {
            final Optional<SessionFactory> sessionFactory = (Optional<SessionFactory>) getSessionFactory();

            if (sessionFactory.isPresent()) {
                new HibernateQueryMetrics(sessionFactory.get(), monitoringProperties.getMeterHibernateName(), Tags.empty()).bindTo(registry);
            } else {
                warn("No session factory to monitor (hibernate query)");
            }
        }

        try {
            if (monitoringProperties.isMeterPostgres()) {
                final Optional<Object> dataSource = (Optional<Object>) getDataSource();
                if (dataSource.isPresent()) {
                    if (monitoringProperties.getPostgresDatabaseName() != null) {
                        new PostgreSQLDatabaseMetrics((DataSource) dataSource.get(), monitoringProperties.getPostgresDatabaseName()).bindTo(registry);
                    } else {
                        log.error("For metering postgres one should provide an existing database name");
                    }
                } else {
                    warn("No datasource to monitor (postgres)");
                }
            }
        } catch (java.lang.NoClassDefFoundError noClassDefFoundError) {
            warn(String.format("No hibernate postgresql metrics. Missing class %s", noClassDefFoundError.getMessage()));
        }


        if (monitoringProperties.isMeterProcessor()) {
            new ProcessorMetrics().bindTo(registry);
        }

        if (monitoringProperties.isMeterCamel()) {
            try {
                if (classForName("org.apache.camel.component.micrometer.routepolicy.MicrometerRoutePolicyFactory").isPresent()) {
                    Class<?> factoryClass = Class.forName("org.apache.camel.component.micrometer.routepolicy.MicrometerRoutePolicyFactory");
                    Object factory = factoryClass.getDeclaredConstructor().newInstance();
                    factoryClass.getMethod("setMeterRegistry", MeterRegistry.class).invoke(factory, meterRegistry);
                    Class<?> camelContextClass = Class.forName("org.apache.camel.CamelContext");
                    Object camelContext;
                    try {
                        camelContext = applicationContext.getBean(camelContextClass);
                    } catch (BeansException e) {
                        camelContext = null;
                    }
                    if (camelContext == null) {
                        log.warn("No camel context found in {}", applicationContext);
                    } else {
                        camelContextClass.getMethod("addRoutePolicyFactory", Class.forName("org.apache.camel.spi.RoutePolicyFactory")).invoke(camelContext, factory);
                        log.info("Set up {}", factory);
                    }
                } else {
                    log.info("No camel micrometer route policy factory found");
                }
            } catch (Exception e) {
                log.warn(e.getClass() + ":" + e.getMessage(), e);
            }
        }


        if (monitoringProperties.isMeterTomcat() && getManager().isPresent()) {
            final Optional<Manager> manager = (Optional<Manager>) getManager();
            TomcatMetrics metrics = new TomcatMetrics(manager.get(), Tags.empty());
            metrics.bindTo(registry);
            closables.add(metrics);
        }
        if (monitoringProperties.isMeterUptime()) {
            new UptimeMetrics().bindTo(registry);
        }
        if (monitoringProperties.getMeterVolumes() != null) {
            for (String folder : monitoringProperties.getMeterVolumes()) {
                File dir = new File(folder);
                if (dir.exists()) {
                    new DiskSpaceMetrics(dir).bindTo(registry);
                } else {
                    log.info("Not metring {}, because it does not exist", dir);
                }
            }
        }
        if (monitoringProperties.isMeterLocks()) {
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

            builder("locks.average_acquiretime",
                    () -> JMX_INSTANCE.getAverageLockAcquireTime().getWindowValue().optionalDoubleMean().orElse(0d)
                )
                .description("The average time in ms to acquire a lock (in " + JMX_INSTANCE.getAverageLockAcquireTime().getTotalDuration() + ")")
                .register(registry);

            Gauge.builder("locks.average_duration",
                    () -> JMX_INSTANCE.getAverageLockDuration().getWindowValue().optionalDoubleMean().orElse(0d)
                )
                .description("The average time in ms to hold a lock (in " + JMX_INSTANCE.getAverageLockDuration().getTotalDuration() + ")")
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
        if (monitoringProperties.meterGaugeScript) {
            String[] lines = monitoringProperties.gaugeScript.trim().split("\n");
            for (String l : lines) {
                String[] split = l.trim().split("\t");
                new ScriptMeterBinder(
                    split[0],
                    split[1].split(","),
                    split[2],
                    Arrays.copyOfRange(split, 3, split.length)).bindTo(registry);
            }
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
        Optional<?> manager = classForName("org.apache.catalina.Manager", DEBUG)
            .flatMap(this::getBean);
        if (manager.isEmpty()) {
            log.info("No tomcat manager found");
        }
        return manager;
    }

    private Optional<Class<?>> classForName(String name) {
        return classForName(name, WARN);
    }


    private Optional<Class<?>> classForName(String name, Level level) {
        try {
            final Class<?> aClass = Class.forName(name, true, this.getClass().getClassLoader());
            return Optional.of(aClass);
        } catch (ClassNotFoundException e) {
            warn("class not found: " + name + ":" + e.getMessage(), level);
            return Optional.empty();
        }
    }

    private void warn(String warn){
        warn(warn, WARN);
    }

    private void warn(String warn, Level level){
        log.atLevel(level).log(warn);
        warnings.add(warn);
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
