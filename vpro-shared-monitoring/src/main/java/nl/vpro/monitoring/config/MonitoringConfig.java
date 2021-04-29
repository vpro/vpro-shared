package nl.vpro.monitoring.config;

import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.cache.EhCache2Metrics;
import io.micrometer.core.instrument.binder.db.PostgreSQLDatabaseMetrics;
import io.micrometer.core.instrument.binder.jpa.HibernateMetrics;
import io.micrometer.core.instrument.binder.jpa.HibernateQueryMetrics;
import io.micrometer.core.instrument.binder.jvm.*;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.core.instrument.binder.tomcat.TomcatMetrics;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import net.sf.ehcache.Ehcache;

import java.io.File;
import java.util.Optional;

import javax.sql.DataSource;

import org.apache.catalina.Manager;
import org.hibernate.SessionFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "nl.vpro.monitoring.config")
public class MonitoringConfig {

    @Autowired(required = false)
    public MonitoringProperties properties = new MonitoringProperties();

    @Autowired
    private ApplicationContext applicationContext;

    @Bean
    @SuppressWarnings("unchecked")
    public PrometheusMeterRegistry globalMeterRegistry() {
        final PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

        if (properties.getCommonTags() != null) {
            registry.config().commonTags(properties.getCommonTags().toArray(new String[0]));
        }
        if (properties.isMeterClassloader()) {
            new ClassLoaderMetrics().bindTo(registry);
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
        final Optional<SessionFactory> sessionFactory = (Optional<SessionFactory>) getSessionFactory();
        if (properties.isMeterHibernate() && sessionFactory.isPresent()) {
            new HibernateMetrics(sessionFactory.get(), properties.getMeterHibernateName(), Tags.empty()).bindTo(registry);
        }
        if (properties.isMeterHibernateQuery() && sessionFactory.isPresent()) {
            new HibernateQueryMetrics(sessionFactory.get(), properties.getMeterHibernateName(), Tags.empty()).bindTo(registry);
        }
        final Optional<DataSource> dataSource = (Optional<DataSource>) getDataSource();
        if (properties.isMeterPostgres() && dataSource.isPresent()) {
            if (properties.getPostgresDatabaseName() != null) {
                new PostgreSQLDatabaseMetrics(dataSource.get(), properties.getPostgresDatabaseName()).bindTo(registry);
            } else {
                throw new MetricsConfigurationException("For metering posgres one should provide an existing database name");
            }
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
        return registry;
    }

    // Without a datasourc arg instead of the wildcard, applicartion startup will fail
    // when this class is not available
    private Optional<?> getDataSource() {
        return classForName("javax.sql.DataSource")
            .flatMap(c -> {
                try {
                    return getBean(c);
                } catch (BeansException e) {
                    return Optional.empty();
                }
            });
    }

    private Optional<?> getSessionFactory() {
        return classForName("org.hibernate.SessionFactory")
            .flatMap(c -> {
                try {
                    return getBean(c);
                } catch (BeansException e) {
                    return Optional.empty();
                }
            });
    }

    private Optional<?> getEhCache() {
        return classForName("net.sf.ehcache.Ehcache")
            .flatMap(c -> {
                try {
                    return getBean(c);
                } catch (BeansException e) {
                    return Optional.empty();
                }
            });
    }

    private Optional<?> getManager() {
        return classForName("org.apache.catalina.Manager")
            .flatMap(c -> {
                try {
                    return getBean(c);
                } catch (BeansException e) {
                    return Optional.empty();
                }
            });
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
            .map(ac -> ac.getBean(clazz));
    }
}
