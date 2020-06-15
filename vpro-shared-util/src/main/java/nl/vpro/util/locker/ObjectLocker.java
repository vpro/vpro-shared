package nl.vpro.util.locker;

import lombok.Lombok;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.management.ObjectName;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.hibernate.SessionFactory;
import org.slf4j.event.Level;

import nl.vpro.jmx.MBeans;
import nl.vpro.logging.Slf4jHelper;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
@Slf4j
public class ObjectLocker {

    public static SessionFactory sessionFactory;

    /**
     * The lock the current thread is holding. It would be suspicious (and a possible cause of dead lock) if that is more than one.
     */

    static final ThreadLocal<List<LockHolder<? extends Serializable>>> HOLDS = ThreadLocal.withInitial(ArrayList::new);

    private static final ObjectLockerAdmin JMX_INSTANCE    = new ObjectLockerAdmin();



    static {
        try {
            MBeans.registerBean(new ObjectName("nl.vpro:name=objectLocker"), JMX_INSTANCE);
        } catch (Throwable t) {
            throw Lombok.sneakyThrow(t);
        }
    }



    public static boolean stricltyOne;
    public static boolean monitor;
    public static Duration maxLockAcquireTime = Duration.ofMinutes(10);

    /**
     * Map key -> ReentrantLock
     */
    static final Map<Serializable, LockHolder<Serializable>> LOCKED_OBJECTS    = new ConcurrentHashMap<>();



    public static <T> T withKeyLock(
        Serializable id,
        @NonNull String reason,
        @NonNull Callable<T> callable) {
        return withObjectLock(id, reason, callable, ObjectLocker.LOCKED_OBJECTS, (o1, o2) -> false);
    }



    public static <T> T withKeyLock(
        Serializable id,
        @NonNull String reason,
        @NonNull Runnable runnable) {
        return withKeyLock(id, reason, () -> {
            runnable.run();
            return null;
        });
    }


    @SneakyThrows
    public static <T, K extends Serializable> T withObjectLock(
        K key,
        @NonNull String reason,
        @NonNull Callable<T> callable,
        @NonNull Map<K, LockHolder<K>> locks,
        BiFunction<K, K, Boolean> comparable
    ) {
        if (key == null) {
            //log.warn("Calling with null mid: {}", reason, new Exception());
            log.warn("Calling with null key: {}", reason);
            return callable.call();
        }
        long nanoStart = System.nanoTime();
        LockHolder<K> lock = acquireLock(nanoStart, key, reason, locks, comparable);
        try {
            return callable.call();
        } finally {
            releaseLock(nanoStart, key, reason, locks, lock);
        }
    }

    @SneakyThrows
    public static <T, K extends Serializable> T withObjectsLock(
        @NonNull Iterable<K> keys,
        @NonNull String reason,
        @NonNull Callable<T> callable,
        @NonNull Map<K, LockHolder<K>> locks,
        BiFunction<K, K, Boolean> comparable
    ) {

        final long nanoStart = System.nanoTime();
        final List<LockHolder<K>> lockList = new ArrayList<>();
        final List<K> copyOfKeys = new ArrayList<>();
        for (K key : keys) {
            if (key != null) {
                lockList.add(acquireLock(nanoStart, key, reason, locks, comparable));
                copyOfKeys.add(key);
            }
        }
        try {
            return callable.call();
        } finally {
            int i = 0;
            for (K key : copyOfKeys) {
                releaseLock(nanoStart, key, reason, locks, lockList.get(i++));
            }
        }
    }
    @SneakyThrows
    private static <T, K extends Serializable> T withObjectsLockIfFree(
        @NonNull Iterable<K> keys,
        @NonNull String reason,
        @NonNull Function<Iterable<K>, T> callable,
        @NonNull Map<K, LockHolder<K>> locks,
        BiFunction<K, K, Boolean> comparable) {

        final long nanoStart = System.nanoTime();
        final List<LockHolder<K>> lockList = new ArrayList<>();
        final List<K> copyOfKeys = new ArrayList<>();
        for (K key : keys) {
            if (key != null) {
                Optional<LockHolder<K>> reentrantLock = acquireLock(nanoStart, key, reason, locks, true, comparable);
                if (reentrantLock.isPresent()) {
                    lockList.add(reentrantLock.get());
                    copyOfKeys.add(key);
                }
            }
        }
        try {
            return callable.apply(copyOfKeys);
        } finally {
            int i = 0;
            for (K key : copyOfKeys) {
                releaseLock(nanoStart, key, reason, locks, lockList.get(i++));
            }
        }
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    private static  <K extends Serializable> Optional<LockHolder<K>> acquireLock(
        long nanoStart,
        K key,
        @NonNull String reason,
        final @NonNull Map<K, LockHolder<K>> locks,
        boolean onlyIfFree,
        BiFunction<K, K, Boolean> comparable) {
        LockHolder<K> holder;
        boolean alreadyWaiting = false;
        synchronized (locks) {
            holder = locks.computeIfAbsent(key, (m) -> {
                log.trace("New lock for " + m);
                List<LockHolder<? extends Serializable>> currentLocks = HOLDS.get();
                if (! currentLocks.isEmpty()) {
                    if (monitor) {
                        if (sessionFactory != null) {
                            if (sessionFactory.getCurrentSession().getTransaction().isActive()) {
                                log.warn("Trying to acquire lock in transaction which active already! {}:{} + {}", summarize(), currentLocks, key);
                            }
                        }
                    }
                    if (stricltyOne && currentLocks.stream()
                        .anyMatch((l) ->
                            key.getClass().isInstance(l.key) && comparable.apply((K) l.key, key)
                        )) {
                        throw new IllegalStateException(String.format("%s Getting a lock on a different key! %s + %s", summarize(), currentLocks.get(0).summarize(), key));
                    } else {
                        log.warn("Getting a lock on a different key! {} + {}", currentLocks, key);
                    }
                }
                LockHolder<K> newHolder = new LockHolder<>(key, reason, new ReentrantLock(), new Exception());
                HOLDS.get().add(newHolder);
                return newHolder;
                }
            );
            if (holder.lock.isLocked() && !holder.lock.isHeldByCurrentThread()) {
                if (onlyIfFree) {
                    return Optional.empty();
                }
                log.debug("There are already threads ({}) for {}, waiting", holder.lock.getQueueLength(), key);
                JMX_INSTANCE.maxConcurrency = Math.max(holder.lock.getQueueLength(), JMX_INSTANCE.maxConcurrency);
                alreadyWaiting = true;
            }

        }

        if (monitor) {
            long start = System.nanoTime();
            Duration wait = Duration.ofSeconds(5);
            Duration maxWait = Duration.ofSeconds(30);
            try {
                while (!holder.lock.tryLock(wait.toMillis(), TimeUnit.MILLISECONDS)) {
                    Duration duration = Duration.ofNanos(System.nanoTime() - start);
                    log.info("Couldn't  acquire lock for {} during {}, {}, locked by {}", key, duration, ObjectLocker.summarize(), holder.summarize());
                    if (duration.compareTo(ObjectLocker.maxLockAcquireTime) > 0) {
                        log.warn("Took over {} to acquire {}, continuing without lock now", ObjectLocker.maxLockAcquireTime, holder);
                        break;
                    }
                    if (wait.compareTo(maxWait) < 0) {
                        wait = wait.multipliedBy(2);
                        if (wait.compareTo(maxWait) > 0) {
                            wait = maxWait;
                        }
                    }
                }
            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);
                Thread.currentThread().interrupt();
                return Optional.empty();

            }
        } else {
            holder.lock.lock();
        }
        if (alreadyWaiting) {
            log.debug("Released and continuing {}", key);
        }

        JMX_INSTANCE.maxDepth = Math.max(JMX_INSTANCE.maxDepth, holder.lock.getHoldCount());
        log.trace("{} holdcount {}", Thread.currentThread().hashCode(), holder.lock.getHoldCount());
        if (holder.lock.getHoldCount() == 1) {
            JMX_INSTANCE.lockCount.computeIfAbsent(reason, (s) -> new AtomicInteger(0)).incrementAndGet();
            JMX_INSTANCE.currentCount.computeIfAbsent(reason, (s) -> new AtomicInteger()).incrementAndGet();
            Duration aquireTime = Duration.ofNanos(System.nanoTime() - nanoStart);
            log.debug("Acquired lock for {}  ({}) in {}", key, reason, aquireTime);
        }
        return Optional.of(holder);
    }

    private static  <K extends Serializable> LockHolder<K> acquireLock(long nanoStart, K key, @NonNull  String reason, final @NonNull Map<K, LockHolder<K>> locks, BiFunction<K, K, Boolean> comparable) {
        return acquireLock(nanoStart, key, reason, locks, false, comparable).orElseThrow(IllegalStateException::new);
    }


    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    private static  <K extends Serializable> void releaseLock(long nanoStart, K key, @NonNull  String reason, final @NonNull Map<K, LockHolder<K>> locks, @NonNull LockHolder<K> lock) {
        synchronized (locks) {
            if (lock.lock.getHoldCount() == 1) {
                if (!lock.lock.hasQueuedThreads()) {
                    log.trace("Removed " + key);
                    LockHolder<K> remove = locks.remove(key);
                    if (remove != null) {


                    }
                }
                JMX_INSTANCE.currentCount.computeIfAbsent(reason, (s) -> new AtomicInteger()).decrementAndGet();
                Duration duration = Duration.ofNanos(System.nanoTime() - nanoStart);
                Slf4jHelper.log(log, duration.compareTo(Duration.ofSeconds(30))> 0 ? Level.WARN :  Level.DEBUG,
                    "Released lock for {} ({}) in {}", key, reason, Duration.ofNanos(System.nanoTime() - nanoStart));
            }
            HOLDS.get().remove(lock);
            lock.lock.unlock();
            locks.notifyAll();
        }
    }


     public static class LockHolder<K> {
        public final K key;
        public final ReentrantLock lock;
        final Exception cause;
        final Thread thread;
        final Instant createdAt = Instant.now();
        final String reason;

        public LockHolder(K k, String reason, ReentrantLock lock, Exception cause) {
            this.key = k;
            this.lock = lock;
            this.cause = cause;
            this.thread = Thread.currentThread();
            this.reason = reason;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            LockHolder<K> holder = (LockHolder) o;

            return key.equals(holder.key);
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }

        /**
         * @since 5.13
         */
        public Duration getAge() {
            return Duration.between(createdAt, Instant.now());
        }

        public String summarize() {
            return key + ":" + createdAt + "(age: " + getAge() + "):" + reason + ":" +
                ObjectLocker.summarize(this.thread, this.cause);
        }

        @Override
        public String toString() {
            return "holder:" + key + ":" + createdAt;
        }
    }


    private static String summarize(Thread t, Exception e) {
        return t.toString() + ":" + summarizeStackTrace(e);

    }
     private static String summarize() {
        return summarize(Thread.currentThread(), new Exception());

    }



    private static String summarizeStackTrace(Exception ex) {
        return "\n" +
            Stream.of(ex.getStackTrace())
                .filter(e -> e.getClassName().startsWith("nl.vpro"))
                .filter(e -> ! e.getClassName().startsWith("nl.vpro.spring"))
                .filter(e -> ! e.getClassName().startsWith("nl.vpro.services"))
                .filter(e -> e.getFileName() != null && ! e.getFileName().contains("generated"))
                .map(StackTraceElement::toString)
                .collect(Collectors.joining("\n   <-"));
    }
}
