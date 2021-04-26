package nl.vpro.monitoring.config;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
public class MonitoringProperties {

    @Value("${monitoring.tags:#{null}}")
    private List<String> commonTags;

    @Value("${monitoring.classloader.enabled:false}")
    private boolean meterClassloader;

    @Value("${monitoring.ehcache2.enabled:false}")
    private boolean meterEhCache2;

    @Value("${monitoring.jvm.heap.enabled:false}")
    private boolean meterJvmHeap;

    @Value("${monitoring.jvm.gc.enabled:false}")
    private boolean meterJvmGc;

    @Value("${monitoring.jvm.memory.enabled:false}")
    private boolean meterJvmMemory;

    @Value("${monitoring.jvm.thread.enabled:false}")
    private boolean meterJvmThread;

    @Value("${monitoring.hibernate.enabled:false}")
    private boolean meterHibernate;

    @Value("${monitoring.hibernate.name:hibernate}")
    private String meterHibernateName;

    @Value("${monitoring.hibernate.query.enabled:false}")
    private boolean meterHibernateQuery;

    @Value("${monitoring.postgres.enabled:false}")
    private boolean meterPostgres;

    @Value("${monitoring.postgres.database.name:#{null}}")
    private String postgresDatabaseName;

    @Value("${monitoring.processor.enabled:false}")
    private boolean meterProcessor;

    @Value("${monitoring.tomcat.enabled:false}")
    private boolean meterTomcat;

    @Value("${monitoring.uptime.enabled:false}")
    private boolean meterUptime;

    @Value("${monitoring.volumes:#{null}}")
    private List<String> meterVolumes;
}
