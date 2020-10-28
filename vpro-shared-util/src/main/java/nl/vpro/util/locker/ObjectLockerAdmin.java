package nl.vpro.util.locker;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import nl.vpro.jmx.MBeans;
import nl.vpro.util.TimeUtils;

/**
 * @author Michiel Meeuwissen
 * @since 5.8
 */
public class ObjectLockerAdmin implements ObjectLockerAdminMXBean {


    public static final ObjectLockerAdmin JMX_INSTANCE    = new ObjectLockerAdmin();


    static {
        try {
            MBeans.registerBean(new ObjectName("nl.vpro:name=objectLocker"), JMX_INSTANCE);
        } catch (MalformedObjectNameException mfoe) {
            // ignored, the objectname _is_ not malformed
        }
    }

   private ObjectLockerAdmin() {

    }

    /**
     * Number of locks per 'reason'.
     */
    Map<String, AtomicInteger> lockCount = new HashMap<>();

    /**
     * Count per 'reason'.
     */
    Map<String, AtomicInteger> currentCount = new HashMap<>();

    @Getter
    int maxConcurrency = 0;

    @Getter
    int maxDepth = 0;

    @Override
    public Set<String> getLocks() {
        return ObjectLocker.LOCKED_OBJECTS.values().stream().map(ObjectLocker.LockHolder::summarize).collect(Collectors.toSet());
    }

    @Override
    public int getLockCount() {
        return lockCount.values().stream().mapToInt(AtomicInteger::intValue).sum();
    }

    @Override
    public Map<String, Integer> getLockCounts() {
        return lockCount.entrySet().stream().collect(
            Collectors.toMap(Map.Entry::getKey, e -> e.getValue().intValue()));
    }

    @Override
    public int getCurrentCount() {
        return currentCount.values().stream().mapToInt(AtomicInteger::intValue).sum();
    }

    @Override
    public Map<String, Integer> getCurrentCounts() {
        return currentCount.entrySet().stream().collect(
            Collectors.toMap(Map.Entry::getKey, e -> e.getValue().intValue()));
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
        return ObjectLocker.stricltyOne;
    }

    @Override
    public void setStrictlyOne(boolean strictlyOne) {
        ObjectLocker.stricltyOne = strictlyOne;
    }
}
