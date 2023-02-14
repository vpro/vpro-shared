package nl.vpro.monitoring.config;

import io.micrometer.core.instrument.ImmutableTag;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import lombok.extern.log4j.Log4j2;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Log4j2
class MonitoringConfigTest {

    @Mock
    private MonitoringProperties properties;

    @InjectMocks
    private MonitoringConfig config;

    private int offset;

    @BeforeEach
    public  void findOffset() {
        offset = config.globalMeterRegistry().getMeters().size(); // some log4j2 counter are present
    }


    @Test
    void empty() {
        final PrometheusMeterRegistry registry = config.globalMeterRegistry();
        assertThat(registry.getMeters()).hasSize(offset);
    }

    @Test
    void classLoader() {
        when(properties.isMeterClassloader()).thenReturn(true);
        final PrometheusMeterRegistry registry = config.globalMeterRegistry();
        assertThat(registry.getMeters()).hasSizeGreaterThanOrEqualTo(offset + 1);
    }

    @Test
    void isMeterEhCache2() {
        when(properties.isMeterEhCache2()).thenReturn(true);
        final PrometheusMeterRegistry registry = config.globalMeterRegistry();
        assertThat(registry.getMeters()).hasSize(offset);
    }

    @Test
    void isMeterJvmHeap() {
        when(properties.isMeterJvmHeap()).thenReturn(true);
        final PrometheusMeterRegistry registry = config.globalMeterRegistry();
        assertThat(registry.getMeters()).hasSizeGreaterThanOrEqualTo(offset + 1);
    }

    @Test
    void isMeterJvmGc() {
        when(properties.isMeterJvmGc()).thenReturn(true);
        final PrometheusMeterRegistry registry = config.globalMeterRegistry();
        assertThat(registry.getMeters()).hasSizeGreaterThanOrEqualTo(offset + 1);
    }

    @Test
    void isMeterJvmMemory() {
        when(properties.isMeterJvmMemory()).thenReturn(true);
        final PrometheusMeterRegistry registry = config.globalMeterRegistry();
        assertThat(registry.getMeters()).hasSizeGreaterThanOrEqualTo(offset + 1);
    }

    @Test
    void isMeterJvmThread() {
        when(properties.isMeterJvmThread()).thenReturn(true);
        final PrometheusMeterRegistry registry = config.globalMeterRegistry();
        assertThat(registry.getMeters()).hasSizeGreaterThanOrEqualTo(offset + 1);
    }

    @Test
    void isMeterHibernate() {
        when(properties.isMeterHibernate()).thenReturn(true);
        final PrometheusMeterRegistry registry = config.globalMeterRegistry();
        assertThat(registry.getMeters()).hasSize(offset);
    }

    @Test
    void isMeterHibernateQuery() {
        when(properties.isMeterHibernateQuery()).thenReturn(true);
        final PrometheusMeterRegistry registry = config.globalMeterRegistry();
        assertThat(registry.getMeters()).hasSize(offset);
    }

    @Test
    void isMeterPostgres() {
        when(properties.isMeterPostgres()).thenReturn(true);
        final PrometheusMeterRegistry registry = config.globalMeterRegistry();
        assertThat(registry.getMeters()).hasSize(offset);
    }

    @Test
    void isMeterProcessor() {
        when(properties.isMeterProcessor()).thenReturn(true);
        final PrometheusMeterRegistry registry = config.globalMeterRegistry();
        assertThat(registry.getMeters()).hasSizeGreaterThanOrEqualTo(offset + 1);
    }

    @Test
    void isMeterTomcat() {
        when(properties.isMeterTomcat()).thenReturn(true);
        final PrometheusMeterRegistry registry = config.globalMeterRegistry();
        assertThat(registry.getMeters()).hasSize(offset);
    }

    @Test
    void isMeterUptime() {
        when(properties.isMeterUptime()).thenReturn(true);
        final PrometheusMeterRegistry registry = config.globalMeterRegistry();
        assertThat(registry.getMeters()).hasSizeGreaterThanOrEqualTo(offset = 1);
    }

    @Test
    void meterVolumesWitTags() {
        when(properties.getCommonTags()).thenReturn(Arrays.asList("tag", "value"));
        when(properties.getMeterVolumes()).thenReturn(Collections.singletonList("/"));
        final PrometheusMeterRegistry registry = config.globalMeterRegistry();
        assertThat(registry.getMeters()).hasSizeGreaterThanOrEqualTo(1);
        assertThat(registry.getMeters().get(0).getId().getTags()).contains(new ImmutableTag("tag", "value"));
        log.info("Found meters {}", registry.getMeters());
    }
}
