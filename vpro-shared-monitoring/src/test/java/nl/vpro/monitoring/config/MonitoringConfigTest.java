package nl.vpro.monitoring.config;

import io.micrometer.core.instrument.ImmutableTag;
import io.micrometer.core.instrument.Tag;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import net.sf.ehcache.Ehcache;

import java.util.*;

import javax.sql.DataSource;

import org.apache.catalina.Manager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonitoringConfigTest {

    @Mock
    private MonitoringProperties properties;

    @InjectMocks
    private MonitoringConfig config;

    @Test
    void empty() {
        final PrometheusMeterRegistry registry = config.globalMeterRegistry(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        assertThat(registry.getMeters()).isEmpty();
    }

    @Test
    void classLoader() {
        when(properties.isMeterClassloader()).thenReturn(true);
        final PrometheusMeterRegistry registry = config.globalMeterRegistry(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        assertThat(registry.getMeters()).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    void isMeterEhCache2() {
        when(properties.isMeterEhCache2()).thenReturn(true);
        final PrometheusMeterRegistry registry = config.globalMeterRegistry(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        assertThat(registry.getMeters()).isEmpty();
    }

    @Test
    void isMeterJvmHeap() {
        when(properties.isMeterJvmHeap()).thenReturn(true);
        final PrometheusMeterRegistry registry = config.globalMeterRegistry(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        assertThat(registry.getMeters()).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    void isMeterJvmGc() {
        when(properties.isMeterJvmGc()).thenReturn(true);
        final PrometheusMeterRegistry registry = config.globalMeterRegistry(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        assertThat(registry.getMeters()).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    void isMeterJvmMemory() {
        when(properties.isMeterJvmMemory()).thenReturn(true);
        final PrometheusMeterRegistry registry = config.globalMeterRegistry(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        assertThat(registry.getMeters()).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    void isMeterJvmThread() {
        when(properties.isMeterJvmThread()).thenReturn(true);
        final PrometheusMeterRegistry registry = config.globalMeterRegistry(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        assertThat(registry.getMeters()).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    void isMeterHibernate() {
        when(properties.isMeterHibernate()).thenReturn(true);
        final PrometheusMeterRegistry registry = config.globalMeterRegistry(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        assertThat(registry.getMeters()).isEmpty();
    }

    @Test
    void isMeterHibernateQuery() {
        when(properties.isMeterHibernateQuery()).thenReturn(true);
        final PrometheusMeterRegistry registry = config.globalMeterRegistry(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        assertThat(registry.getMeters()).isEmpty();
    }

    @Test
    void isMeterPostgres() {
        when(properties.isMeterPostgres()).thenReturn(true);
        final PrometheusMeterRegistry registry = config.globalMeterRegistry(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        assertThat(registry.getMeters()).isEmpty();
    }

    @Test
    void isMeterPostgresWithDataSource() {
        when(properties.isMeterPostgres()).thenReturn(true);
        when(properties.getPostgresDatabaseName()).thenReturn("TEST");
        final PrometheusMeterRegistry registry = config.globalMeterRegistry(Optional.of(Mockito.mock(DataSource.class)), Optional.empty(), Optional.empty(), Optional.empty());
        assertThat(registry.getMeters()).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    void isMeterProcessor() {
        when(properties.isMeterProcessor()).thenReturn(true);
        final PrometheusMeterRegistry registry = config.globalMeterRegistry(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        assertThat(registry.getMeters()).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    void isMeterTomcat() {
        when(properties.isMeterTomcat()).thenReturn(true);
        final PrometheusMeterRegistry registry = config.globalMeterRegistry(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        assertThat(registry.getMeters()).isEmpty();
    }

    @Test
    void isMeterUptime() {
        when(properties.isMeterUptime()).thenReturn(true);
        final PrometheusMeterRegistry registry = config.globalMeterRegistry(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        assertThat(registry.getMeters()).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    void meterVolumesWitTags() {
        when(properties.getCommonTags()).thenReturn(Arrays.asList("tag", "value"));
        when(properties.getMeterVolumes()).thenReturn(Collections.singletonList("volume"));
        final PrometheusMeterRegistry registry = config.globalMeterRegistry(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        assertThat(registry.getMeters()).hasSizeGreaterThanOrEqualTo(1);
        assertThat(registry.getMeters().get(0).getId().getTags()).contains(new ImmutableTag("tag", "value"));
    }
}
