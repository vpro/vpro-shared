package nl.vpro.monitoring.binder;

import io.micrometer.common.lang.NonNull;
import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.binder.BaseUnits;
import io.micrometer.core.instrument.binder.MeterBinder;

import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicLong;

import com.sun.management.HotSpotDiagnosticMXBean;
import com.sun.management.VMOption;

import static java.util.Collections.emptyList;

public class JvmMaxDirectMemorySize implements MeterBinder {

    private static final String MAX_DIRECT_MEMORY_SIZE = "MaxDirectMemorySize";
    private final Iterable<Tag> tags;
    private final AtomicLong maxDirectMemorySize = new AtomicLong();

    public JvmMaxDirectMemorySize() {
        this(emptyList());
    }

    public JvmMaxDirectMemorySize(Iterable<Tag> tags) {
        this.tags = tags;
    }


    @Override
    public void bindTo(@NonNull MeterRegistry registry) {
        setMaxDirectMemorySize();
        Iterable<Tag> tagsWithId = Tags.concat(tags, "id", "direct");

        Gauge.builder("jvm.buffer.memory.max.bytes", maxDirectMemorySize, AtomicLong::get)
            .tags(tagsWithId)
            .description("The maximum number of bytes that can be allocated to direct memory")
            .baseUnit(BaseUnits.BYTES)
            .strongReference(true)
            .register(registry);

    }

    private void setMaxDirectMemorySize() {

        HotSpotDiagnosticMXBean diagnosticMXBean = ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);
        maxDirectMemorySize.set(0);
        if (diagnosticMXBean != null) {
            VMOption option = diagnosticMXBean.getVMOption(MAX_DIRECT_MEMORY_SIZE);
            if (option != null) {
                try {
                    maxDirectMemorySize.set(Long.parseLong(option.getValue()));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (maxDirectMemorySize.get() == 0) {
            maxDirectMemorySize.set(Runtime.getRuntime().maxMemory());
        }
    }
}
