package nl.vpro.monitoring.config;

import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
public class MonitoringProperties {

    @Value("${monitoring.user:${MONITORING_USER:manager}}")
    private String user;

    @Value("${monitoring.password:${MONITORING_PASSWORD:admin2k}}")
    private String password;

    @Value("${monitoring.tags:#{null}}")
    private List<String> commonTags;

    @Value("${monitoring.log4j.enabled:true}")
    private boolean meterLog4j;

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


    @Value("${monitoring.camel.enabled:true}")
    private boolean meterCamel;

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

    @Value("${data.dir:/data}")
    String dataDir;

    @Value("${monitoring.unhealthyThreshold:10s}")
    String unhealthyThreshold = Duration.ofSeconds(10).toString();


    @Value("${monitoring.minThreadDumpInterval:1h}")
    String minThreadDumpInterval = Duration.ofHours(1).toString();


    @Value("${monitoring.gaugeScript.enabled:${MONITORING_HAS_SCRIPTS:false}}")
    boolean meterGaugeScript;

    @Value("""
    ${monitoring.gaugeScript:
       1h\t/scripts/parse_access_logs.pl\t7d
       5m\t/scripts/parse_access_logs.pl\t1h
       1h\t/scripts/parse_tomcat_access_logs.pl\t7d
       5m\t/scripts/parse_tomcat_access_logs.pl\t1h
    }""")
    String gaugeScript;

}
