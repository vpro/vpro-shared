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

    @Value("${monitoring.user}")
    private String user;

    @Value("${monitoring.password}")
    private String password;

    @Value("${monitoring.tags:#{null}}")
    private List<String> commonTags;

    @Value("${monitoring.classloader.enabled:true}")
    private boolean meterClassloader;

    @Value("${monitoring.jcache.enabled:true}")
    private boolean meterJCache;

    @Value("${monitoring.jvm.heap.enabled:true}")
    private boolean meterJvmHeap;

    @Value("${monitoring.jvm.gc.enabled:true}")
    private boolean meterJvmGc;

    @Value("${monitoring.jvm.memory.enabled:true}")
    private boolean meterJvmMemory;

    @Value("${monitoring.jvm.thread.enabled:true}")
    private boolean meterJvmThread;

    @Value("${monitoring.hibernate.enabled:true}")
    private boolean meterHibernate;

    @Value("${monitoring.hibernate.name:hibernate}")
    private String meterHibernateName;

    @Value("${monitoring.hibernate.query.enabled:false}") // ''Be aware of the potential for high cardinality of unique Hibernate queries executed by your application'
    private boolean meterHibernateQuery;

    @Value("${monitoring.postgres.enabled:true}")
    private boolean meterPostgres;

    @Value("${monitoring.postgres.database.name:#{null}}")
    private String postgresDatabaseName;

    @Value("${monitoring.processor.enabled:true}")
    private boolean meterProcessor;

    @Value("${monitoring.tomcat.enabled:true}")
    private boolean meterTomcat;

    @Value("${monitoring.uptime.enabled:true}")
    private boolean meterUptime;

    @Value("${monitoring.volumes:#{null}}")
    private List<String> meterVolumes;

    @Value("${monitoring.locks:true}")
    private boolean meterLocks;

    @Value("${monitoring.health.permitAll:true}")
    private boolean healthPermitAll;
}
