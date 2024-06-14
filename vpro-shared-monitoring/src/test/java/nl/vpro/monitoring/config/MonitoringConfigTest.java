package nl.vpro.monitoring.config;

import io.micrometer.core.instrument.ImmutableTag;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import lombok.extern.log4j.Log4j2;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@Log4j2
class MonitoringConfigTest {
    @Spy
    private MonitoringProperties properties;

    @Mock
    private ApplicationContext applicationContext;

    @InjectMocks
    private MonitoringConfig config;

      @BeforeEach
    public  void findOffset() {
        config.init(null);
        config.getGlobalMeterRegistry().clear();
    }


    @Test
    void empty() {
        final PrometheusMeterRegistry registry = config.configure();
        assertThat(registry.getMeters()).hasSize(0);
        assertThat(config.getWarnings()).hasSize(0);

    }

    @Test
    void classLoader() {
        properties.setMeterClassloader(true);
        final PrometheusMeterRegistry registry = config.configure();
        assertThat(registry.getMeters()).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    void isMeterJCache() {
        properties.setMeterJCache(true);
        final PrometheusMeterRegistry registry = config.configure();
        assertThat(registry.getMeters()).hasSize(0); // still 0, because no cache is configured
    }

    @Test
    void isMeterJvmHeap() {
        properties.setMeterJvmHeap(true);
        final PrometheusMeterRegistry registry = config.configure();
        assertThat(registry.getMeters()).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    void isMeterJvmGc() {
        properties.setMeterJvmGc(true);
        final PrometheusMeterRegistry registry = config.configure();
        assertThat(registry.getMeters()).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    void isMeterJvmMemory() {
        properties.setMeterJvmMemory(true);
        final PrometheusMeterRegistry registry = config.configure();
        assertThat(registry.getMeters()).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    void isMeterJvmThread() {
        properties.setMeterJvmThread(true);
        final PrometheusMeterRegistry registry = config.configure();
        assertThat(registry.getMeters()).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    void isMeterHibernate() {
        properties.setMeterHibernate(true);
        final PrometheusMeterRegistry registry = config.configure();
        assertThat(config.getWarnings()).hasSize(1);
    }

    @Test
    void isMeterHibernateQuery() {
        properties.setMeterHibernateQuery(true);
        final PrometheusMeterRegistry registry = config.configure();
        assertThat(config.getWarnings()).hasSize(1);
    }

    @Test
    void isMeterPostgres() {
        properties.setMeterPostgres(true);
        final PrometheusMeterRegistry registry = config.configure();
        assertThat(config.getWarnings()).hasSize(1);
    }

    @Test
    void isMeterProcessor() {
        properties.setMeterProcessor(true);
        final PrometheusMeterRegistry registry = config.configure();
        assertThat(registry.getMeters()).hasSizeGreaterThanOrEqualTo( 1);
    }

    @Test
    void isMeterTomcat() {
        properties.setMeterTomcat(true);
        final PrometheusMeterRegistry registry = config.configure();
        assertThat(config.getWarnings()).hasSize(0);
    }

    @Test
    void isMeterUptime() {
        properties.setMeterUptime(true);
        final PrometheusMeterRegistry registry = config.configure();
        assertThat(registry.getMeters()).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    void meterVolumesWitTags() {
        properties.setCommonTags(Arrays.asList("tag", "value"));
        properties.setMeterVolumes(Collections.singletonList("/"));

        final PrometheusMeterRegistry registry = config.configure();
        assertThat(registry.getMeters())
            .hasSizeGreaterThanOrEqualTo(1);
        assertThat(registry.getMeters()
            .get(registry.getMeters().size() - 1).getId().getTags()).contains(new ImmutableTag("tag", "value"));

        log.info("Found meters {}", registry.getMeters());
    }
}
