package nl.vpro.monitoring.config;

import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.PostConstruct;

@Deprecated
@Slf4j
public class MonitoringConfig {

    @PostConstruct
    public void init() {
        log.warn("""
            MonitoringConfig is deprecated.
            Monitoring end points can be set up via monitoring_endpoints web-fragment
            web.xml should contain:
             <absolute-ordering>
                <name>monitoring_endpoints</name>
              </absolute-ordering>
              'manage' servlet should not be explicitly mapped any more.
            """
        );

    }
}
