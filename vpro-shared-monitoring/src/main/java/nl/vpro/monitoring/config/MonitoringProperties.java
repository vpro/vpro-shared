package nl.vpro.monitoring.config;

import jakarta.annotation.PostConstruct;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.util.*;

import lombok.extern.log4j.Log4j2;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@Slf4j
public class MonitoringProperties implements Serializable {

    @Serial
    private static final long serialVersionUID = -1602387003789243992L;

    @Value("${monitoring.user:${MONITORING_USER:manager}}")
    private String user;

    @Value("${monitoring.password:${MONITORING_PASSWORD:#{null}}}")
    private String password;

    /**
     * File containing the bearer token of the service account of the (OpenShift/Kubernetes) deployment.
     * An incoming {@code Authorization: Bearer <token>} is accepted when it equals the contents of this file.
     * Defaults to the standard location where Kubernetes mounts the pod's service account token.
     * When the file is absent or unreadable, service-account (bearer) authentication is simply disabled.
     */
    @Value("${monitoring.serviceTokenFile:${MONITORING_SERVICE_TOKEN_FILE:/var/run/secrets/kubernetes.io/serviceaccount/token}}")
    private String serviceTokenFile;

    @Value("${monitoring.tags:#{null}}")
    private List<String> commonTags;

    @Value("${monitoring.log4j.enabled:#{null}}")
    private Boolean meterLog4j;

    @Value("${monitoring.classloader.enabled:true}")
    private boolean meterClassloader;

    @Value("${monitoring.jcache.enabled:#{null}}")
    private Boolean meterJCache;

    @Value("${monitoring.jvm.heap.enabled:true}")
    private boolean meterJvmHeap;

    @Value("${monitoring.jvm.gc.enabled:true}")
    private boolean meterJvmGc;

    @Value("${monitoring.jvm.memory.enabled:true}")
    private boolean meterJvmMemory;

    @Value("${monitoring.jvm.thread.enabled:true}")
    private boolean meterJvmThread;


    @Value("${monitoring.camel.enabled:#{null}}")
    private Boolean meterCamel;

    @Value("${monitoring.hibernate.enabled:#{null}}")
    private Boolean meterHibernate;

    @Value("${monitoring.hibernate.name:hibernate}")
    private String meterHibernateName;

    @Value("${monitoring.hibernate.query.enabled:#{null}}") // ''Be aware of the potential for high cardinality of unique Hibernate queries executed by your application'
    private Boolean meterHibernateQuery;

    @Value("${monitoring.postgres.enabled:#{null}}")
    private Boolean meterPostgres;

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

    @Value("${monitoring.locks.enabled:false}")
    private boolean meterLocks;

    @Value("${monitoring.health.permitAll:true}")
    private boolean healthPermitAll;

    @Value("${data.dir:#{null}}")
    String dataDir;

    @Value("${monitoring.unhealthyThreshold:10s}")
    String unhealthyThreshold = Duration.ofSeconds(10).toString();

    @Value("${monitoring.unhealthyCount:6}")
    int unhealthyCount = 6;


    @Value("${monitoring.minThreadDumpInterval:1h}")
    String minThreadDumpInterval = Duration.ofHours(1).toString();


    @Value("${monitoring.gaugeScript.enabled:${MONITORING_HAS_SCRIPTS:#{null}}}")
    Boolean meterGaugeScript;

    @Value("""
    ${monitoring.gaugeScript:
       1h\t/scripts/parse_access_logs.pl\t7d
       5m\t/scripts/parse_access_logs.pl\t1h
       1h\t/scripts/parse_tomcat_access_logs.pl\t7d
       5m\t/scripts/parse_tomcat_access_logs.pl\t1h
    }""")
    String gaugeScript;

    @Value("${monitoring.endpoints.health:/manage/health}")
    private String health = "/manage/health";

    @Value("${monitoring.endpoints.metrics:/manage/metrics}")
    private String metrics = "/manage/metrics";

    @Value("${monitoring.endpoints.prometheus:/manage/prometheus}")
    private String prometheus = "/manage/prometheus";

    @Value("${monitoring.endpoints.wellknown:#{null}}")
    private Boolean wellknown = null;


    public enum Method {
        BASIC,
        BEARER
    }

    private Set<Method> authenticationMethods;

    @PostConstruct
    public void init() {
        Set<Method> authenticationMethods = new HashSet<>();
        if (StringUtils.isNotBlank(getPassword()) && StringUtils.isNotBlank(getUser())) {
            authenticationMethods.add(Method.BASIC);
        }
        if (StringUtils.isNotBlank(getServiceTokenFile())) {
            authenticationMethods.add(Method.BEARER);
        }
        this.authenticationMethods = Collections.unmodifiableSet(authenticationMethods);
        log.info("Available methods for authentication {}", this.authenticationMethods + (healthPermitAll ? "(/health is always permitted)" : ""));

    }
}
