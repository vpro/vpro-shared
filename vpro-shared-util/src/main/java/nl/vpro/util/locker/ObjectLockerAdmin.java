package nl.vpro.util.locker;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.annotation.PreDestroy;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.meeuw.math.statistics.StatisticalLong;
import org.meeuw.math.windowed.WindowedEventRate;
import org.meeuw.math.windowed.WindowedStatisticalLong;

import nl.vpro.jmx.MBeans;
import nl.vpro.util.TimeUtils;

/**
 * @author Michiel Meeuwissen
 * @since 5.8
 */
@Slf4j
public class ObjectLockerAdmin implements ObjectLockerAdminMXBean {


    public static final ObjectLockerAdmin JMX_INSTANCE    = new ObjectLockerAdmin();


    static {
        try {
            MBeans.registerBean(new ObjectName("nl.vpro:name=objectLocker"), JMX_INSTANCE);
        } catch (MalformedObjectNameException mfoe) {
            // ignored, the objectname _is_ not malformed
        }
    }


    @Getter
    final WindowedStatisticalLong averageLockAcquireTime = WindowedStatisticalLong.builder()
        .mode(StatisticalLong.Mode.DURATION)
        .window(Duration.ofMinutes(60))
        .bucketCount(60)
        .build();

    @Getter
    final WindowedEventRate lockRate = WindowedEventRate.builder()
        .window(Duration.ofMinutes(10))
        .bucketCount(60)
        .build();


   private ObjectLockerAdmin() {
       ObjectLocker.listen((type, holder, duration) -> {
           switch(type) {
               case LOCK:
                   maxDepth = Math.max(maxDepth, holder.lock.getHoldCount());

                   if (holder.lock.isLocked() && !holder.lock.isHeldByCurrentThread()) {
                       log.debug("There are already threads ({}) for {}, waiting", holder.lock.getQueueLength(), holder.key);
                       maxConcurrency = Math.max(holder.lock.getQueueLength(), maxConcurrency);
                   }
                   if (holder.lock.getHoldCount() == 1) {
                       lockCount.computeIfAbsent(holder.reason, s -> new AtomicInteger()).incrementAndGet();
                       currentCount.computeIfAbsent(holder.reason, s -> new AtomicInteger()).incrementAndGet();
                       lockRate.newEvent();
                       averageLockAcquireTime.accept(duration.toMillis());
                   }
                   break;
               case UNLOCK:
                   currentCount.computeIfAbsent(holder.reason, s -> new AtomicInteger()).decrementAndGet();

           }
       });
    }

    /**
     * Total number of locks per 'reason'. Never decreases
     */
    private final Map<String, AtomicInteger> lockCount = new HashMap<>();

    /**
     * Current count per 'reason'.
     */
    Map<String, AtomicInteger> currentCount = new HashMap<>();

    @Getter
    private int maxConcurrency = 0;

    @Getter
    private int maxDepth = 0;

    @Override
    public void resetMaxValues() {
        maxConcurrency = 0;
        maxDepth = 0;
    }

    @Override
    public void reset() {
        resetMaxValues();
        lockCount.clear();
        currentCount.clear();
    }


    @Override
    public Set<String> getLocks() {
        return Collections.unmodifiableSet(ObjectLocker.LOCKED_OBJECTS.values().stream()
            .map(ObjectLocker.LockHolder::summarize).collect(Collectors.toSet()));
    }

    @Override
    public int getLockCount() {
        return lockCount.values().stream().mapToInt(AtomicInteger::intValue).sum();
    }

    @Override
    public Map<String, Integer> getLockCounts() {
        return Collections.unmodifiableMap(lockCount.entrySet().stream().collect(
            Collectors.toMap(Map.Entry::getKey, e -> e.getValue().intValue())));
    }

    @Override
    public int getCurrentCount() {
        return currentCount.values().stream().mapToInt(AtomicInteger::intValue).sum();
    }

    @Override
    public Map<String, Integer> getCurrentCounts() {
        return Collections.unmodifiableMap(currentCount.entrySet().stream().collect(
            Collectors.toMap(Map.Entry::getKey, e -> e.getValue().intValue())));
    }

    @Override
    public String getMaxLockAcquireTime() {
        return ObjectLocker.maxLockAcquireTime.toString();
    }

    @Override
    public void setMaxLockAcquireTime(String duration) {
        ObjectLocker.maxLockAcquireTime = TimeUtils.parseDuration(duration).orElse(ObjectLocker.maxLockAcquireTime);
        if (ObjectLocker.minWaitTime.compareTo(ObjectLocker.maxLockAcquireTime.dividedBy(4)) > 0) {
            ObjectLocker.minWaitTime = ObjectLocker.maxLockAcquireTime.dividedBy(4);
        }
    }

    @Override
    public boolean isMonitor() {
        return ObjectLocker.monitor;
    }

    @Override
    public void setMonitor(boolean monitor) {
        ObjectLocker.monitor = monitor;
    }

    @Override
    public boolean isStrictlyOne() {
        return ObjectLocker.strictlyOne;
    }

    @Override
    public void setStrictlyOne(boolean strictlyOne) {
        ObjectLocker.strictlyOne = strictlyOne;
    }

    @PreDestroy
    public void shutdown() {
        lockRate.close();
    }
}
