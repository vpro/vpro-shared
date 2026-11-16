package nl.vpro.monitoring.binder;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class NativeMemoryMetricsTest {

    static final String NOT_ENABLED = "Native memory tracking is not enabled\n";

    // trimmed real -XX:NativeMemoryTracking=summary output (JDK 25), including the 'Shared class space'
    // line, which has an extra 'readonly=' field before the closing paren.
    static final String SUMMARY = """

        Native Memory Tracking:

        (Omitting categories weighting less than 1KB)

        Total: reserved=4314351KB, committed=271791KB
               malloc: 23267KB #55112, peak=33342KB #52280
               mmap:   reserved=4292224KB, committed=249664KB

        -                 Java Heap (reserved=2805760KB, committed=178176KB)
                                    (mmap: reserved=2805760KB, committed=178176KB, at peak)

        -                    Thread (reserved=20543KB, committed=811KB)
                                    (stack: reserved=20480KB, committed=748KB, peak=748KB)

        -        Shared class space (reserved=16384KB, committed=13912KB, readonly=0KB)
                                    (mmap: reserved=16384KB, committed=13912KB, peak=14120KB)

        -           Object Monitors (reserved=1KB, committed=1KB)
        """;

    @Test
    void parsesSummaryIntoGauges() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        NativeMemoryMetrics binder = new NativeMemoryMetrics() {
            @Override
            String fetchSummary() {
                return SUMMARY;
            }
        };
        binder.bindTo(registry);
        binder.close();

        assertThat(registry.get("jvm.native.memory.reserved.bytes").tag("category", "Thread").gauge().value())
            .isEqualTo(20543 * 1024.0);
        assertThat(registry.get("jvm.native.memory.committed.bytes").tag("category", "Thread").gauge().value())
            .isEqualTo(811 * 1024.0);

        assertThat(registry.get("jvm.native.memory.reserved.bytes").tag("category", "Shared class space").gauge().value())
            .isEqualTo(16384 * 1024.0);
        assertThat(registry.get("jvm.native.memory.committed.bytes").tag("category", "Shared class space").gauge().value())
            .isEqualTo(13912 * 1024.0);

        assertThat(registry.get("jvm.native.memory.reserved.bytes").tag("category", "Total").gauge().value())
            .isEqualTo(4314351 * 1024.0);
        assertThat(registry.get("jvm.native.memory.committed.bytes").tag("category", "Total").gauge().value())
            .isEqualTo(271791 * 1024.0);
    }

    @Test
    void registersNothingWhenNotEnabled() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        NativeMemoryMetrics binder = new NativeMemoryMetrics() {
            @Override
            String fetchSummary() {
                return NOT_ENABLED;
            }
        };
        binder.bindTo(registry);
        binder.close();

        assertThat(registry.getMeters().size()).isEqualTo(0);
    }

    @Test
    void updatesExistingGaugesOnRerun() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        NativeMemoryMetrics binder = new NativeMemoryMetrics() {
            @Override
            String fetchSummary() {
                return SUMMARY;
            }
        };
        binder.bindTo(registry);

        binder.update(SUMMARY.replace("reserved=20543KB, committed=811KB", "reserved=30000KB, committed=900KB"));
        binder.close();

        assertThat(registry.get("jvm.native.memory.reserved.bytes").tag("category", "Thread").gauge().value())
            .isEqualTo(30000 * 1024.0);
    }
}
