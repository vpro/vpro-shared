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
import java.util.Collections;
import java.util.Optional;

import javax.sql.DataSource;

import org.apache.catalina.Manager;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import nl.vpro.jackson2.Jackson2Mapper;
import nl.vpro.monitoring.web.HealthController;
import nl.vpro.monitoring.web.PrometheusController;

@Configuration
@ComponentScan(basePackages = "nl.vpro.monitoring.config")
public class MonitoringConfig {

    @Autowired
    public MonitoringProperties properties;

    @Bean
    public PrometheusMeterRegistry globalMeterRegistry(
        Optional<DataSource> dataSource,
        Optional<SessionFactory> sessionFactory,
        Optional<Ehcache> ehCache,
        Optional<Manager> manager
    ) {
        final PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

        if (properties.getCommonTags() != null) {
            registry.config().commonTags(properties.getCommonTags().toArray(new String[0]));
        }
        if (properties.isMeterClassloader()) {
            new ClassLoaderMetrics().bindTo(registry);
        }
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
        if (properties.isMeterHibernate() && sessionFactory.isPresent()) {
            new HibernateMetrics(sessionFactory.get(), properties.getMeterHibernateName(), Tags.empty()).bindTo(registry);
        }
        if (properties.isMeterHibernateQuery() && sessionFactory.isPresent()) {
            new HibernateQueryMetrics(sessionFactory.get(), properties.getMeterHibernateName(), Tags.empty()).bindTo(registry);
        }
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
}
