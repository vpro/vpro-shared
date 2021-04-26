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
public class MonitoringEndpoints {

    @Bean
    public HealthController healthController() {
        return new HealthController();
    }

    @Bean
    public RequestMappingHandlerAdapter requestMappingHandlerAdapter() {
        final RequestMappingHandlerAdapter adapter = new RequestMappingHandlerAdapter();
        adapter.setMessageConverters(Collections.singletonList(new MappingJackson2HttpMessageConverter(Jackson2Mapper.getLenientInstance())));
        return adapter;
    }

    @Bean
    public PrometheusController prometheusController(PrometheusMeterRegistry registry) {
        return new PrometheusController(registry);
    }
}
