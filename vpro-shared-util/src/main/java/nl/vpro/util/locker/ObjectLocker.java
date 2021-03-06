package nl.vpro.util.locker;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.event.Level;

import nl.vpro.logging.Slf4jHelper;

/**
 * @author Michiel Meeuwissen
 */
@Slf4j
public class ObjectLocker {

    private ObjectLocker() {
        // private constructor to avoid all instantiation
    }

    /**
     * The lock the current thread is holding. It would be suspicious (and a possible cause of dead lock) if that is more than one.
     */
    static final ThreadLocal<List<LockHolder<? extends Serializable>>> HOLDS = ThreadLocal.withInitial(ArrayList::new);


    static boolean strictlyOne;
    static boolean monitor;
    static Duration maxLockAcquireTime = Duration.ofMinutes(10);
    static Duration minWaitTime  = Duration.ofSeconds(5);


    private static final List<Listener> LISTENERS = new CopyOnWriteArrayList<>();


    /**
     * You can register {@link Listener}s for lock events, for logging or other reporting purposes
     */
    public static void listen(Listener listener){
        LISTENERS.add(listener);
    }

    /**
     * The reverse of {@link #listen(Listener)}
     */
    public static void unlisten(Listener listener){
        LISTENERS.remove(listener);
    }


    @FunctionalInterface
    public interface Listener extends EventListener {
        enum Type {
            LOCK,
            UNLOCK
        }

        /**
         * Called for lock event
         * @param type What happens to the lock (lock, or unlock)
         * @param holder The relevant {@link LockHolder}
         * @param duration How long it took to obtain/release this lock
         */
        void event(Type type, LockHolder<?> holder, Duration duration);

        default void lock(LockHolder<?> lock, Duration duration) {
            event(Type.LOCK, lock, duration);
        }

        default void unlock(LockHolder<?> lock, Duration duration) {
            event(Type.UNLOCK, lock, duration);
        }
    }

    public interface DefinesType {
        Object getType();
    }

    /**
     * Map key -> LockHolder
     */
    static final Map<Serializable, LockHolder<Serializable>> LOCKED_OBJECTS    = new ConcurrentHashMap<>();


    public static Map<Serializable, LockHolder<Serializable>> getLockedObjects() {
        return Collections.unmodifiableMap(LOCKED_OBJECTS);
    }

    public static <T> T withKeyLock(
        Serializable id,
        @NonNull String reason,
        @NonNull Callable<T> callable) {
        return withObjectLock(id, reason, callable, ObjectLocker.LOCKED_OBJECTS, (o1, o2) -> Objects.equals(o1.getClass(), o2.getClass()));
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


    /**
     * @param key The key to lock on
     * @param reason A description for the reason of locking, which can be used in logging or exceptions
     * @param locks The map to hold the locks
     * @param comparable this determines whether two given keys are 'comparable'. If they are comparable, but different, and occuring at the same time, than this means
     *                   that some code is locking different things at the same time, which may be a cause of dead locks. If they are difference but also not comparable, then
     *                   this remains unknown. We may e.g. be locking on a certain crid and on a mid. They are different, but it is not sure that they are actually about two different objects.
     */

    @SneakyThrows
    public static <T, K extends Serializable> T withObjectLock(
        K key,
        @NonNull String reason,
        @NonNull Callable<T> callable,
        @NonNull Map<K, LockHolder<K>> locks,
        @NonNull BiPredicate<Serializable, K> comparable
    ) {
        if (key == null) {
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


    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    private static  <K extends Serializable> LockHolder<K> acquireLock(
        long nanoStart,
        final  K key,
        final @NonNull String reason,
        final @NonNull Map<K, LockHolder<K>> locks,
        final @NonNull BiPredicate<Serializable, K> comparable) throws InterruptedException {
        LockHolder<K> holder;
        boolean alreadyWaiting = false;
        synchronized (locks) {
            holder = locks.computeIfAbsent(key, (m) -> computeLock(m, reason, comparable));
            if (holder.lock.isLocked() && !holder.lock.isHeldByCurrentThread()) {
                log.debug("There are already threads ({}) for {}, waiting", holder.lock.getQueueLength(), key);
                alreadyWaiting = true;
            }
        }

        if (monitor) {
            monitoredLock(holder, key);
        } else {
            holder.lock.lock();
        }
        if (alreadyWaiting) {
            log.debug("Released and continuing {}", key);
        }

        log.trace("{} holdcount {}", Thread.currentThread().hashCode(), holder.lock.getHoldCount());
        Duration acquireTime = Duration.ofNanos(System.nanoTime() - nanoStart);
        if (holder.lock.getHoldCount() == 1) {
            log.debug("Acquired lock for {}  ({}) in {}", key, reason, acquireTime);
        }


        for(Listener listener : LISTENERS) {
            listener.lock(holder, acquireTime);
        }
        return holder;
    }

    private static  <K extends Serializable> void monitoredLock(LockHolder<K> holder, K key) throws InterruptedException {
        long start = System.nanoTime();
        Duration wait =  minWaitTime;
        Duration maxWait = minWaitTime.multipliedBy(8);
        while (!holder.lock.tryLock(wait.toMillis(), TimeUnit.MILLISECONDS)) {
            Duration duration = Duration.ofNanos(System.nanoTime() - start);
            log.info("Couldn't acquire lock for {} during {}, {}, locked by {}", key, duration, ObjectLocker.summarize(), holder.summarize());
            if (duration.compareTo(ObjectLocker.maxLockAcquireTime) > 0) {
                log.warn("Took over {} to acquire {}, continuing without lock now", ObjectLocker.maxLockAcquireTime, holder);
                break;
            }
            if (wait.compareTo(maxWait) < 0) {
                wait = wait.multipliedBy(2);
            }
            log.info("Wait {}", wait);
        }
    }

    private static <K extends Serializable>  LockHolder<K> computeLock(K key, String reason,
                                                                       @NonNull BiPredicate<Serializable, K> comparable) {
        log.trace("New lock for {}", key);
        List<LockHolder<? extends Serializable>> currentLocks = HOLDS.get();
        if (! currentLocks.isEmpty()) {
            Optional<LockHolder<? extends Serializable>> compatibleLocks = currentLocks.stream().filter(l -> comparable.test(l.key, key)).findFirst();
            if (compatibleLocks.isPresent()) {
                if (strictlyOne) {
                    throw new IllegalStateException(String.format("%s Getting a lock on a different key! %s + %s", summarize(), compatibleLocks.get().summarize(), key));
                } else {
                    log.warn("Getting a lock on a different key! {} + {}", compatibleLocks.get().summarize(), key);
                }
            } else {
                log.debug("Getting a lock on a different (incompatible) key! {} + {}", currentLocks.get(0).key, key);

            }
        }
        LockHolder<K> newHolder = new LockHolder<>(key, reason, new ReentrantLock(), new Exception());
        HOLDS.get().add(newHolder);
        return newHolder;
    }



    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    private static  <K extends Serializable> void releaseLock(long nanoStart, K key, @NonNull  String reason, final @NonNull Map<K, LockHolder<K>> locks, @NonNull LockHolder<K> lock) {
        synchronized (locks) {
            if (lock.lock.getHoldCount() == 1) {
                if (!lock.lock.hasQueuedThreads()) {
                    log.trace("Removed {}", key);
                    locks.remove(key);
                }
                Duration duration = Duration.ofNanos(System.nanoTime() - nanoStart);
                for (Listener listener : LISTENERS) {
                    listener.unlock(lock, duration);
                }

                Slf4jHelper.log(log, duration.compareTo(Duration.ofSeconds(30))> 0 ? Level.WARN :  Level.DEBUG,
                    "Released lock for {} ({}) in {}", key, reason, Duration.ofNanos(System.nanoTime() - nanoStart));
            }
            HOLDS.get().remove(lock);
            if (lock.lock.isHeldByCurrentThread()) { // MSE-4946
                lock.lock.unlock();
            } else {
                // can happen if 'continuing without lock'
                log.warn("Current lock {} not hold by current thread but by {}", lock, lock.thread.getName());
            }

            locks.notifyAll();
        }
    }


    /**
     *  Most importantly this is a wrapper around {@link ReentrantLock}, but it stores some extra meta information, like the original key, and initialization time.
     *
     *  It can also store the exception if that happened during the hold of the lock.
     */
    public static class LockHolder<K> {
        public final K key;
        public final ReentrantLock lock;
        final Exception cause;
        final Thread thread;
        final Instant createdAt = Instant.now();
        final String reason;

        LockHolder(K k, String reason, ReentrantLock lock, Exception cause) {
            this.key = k;
            this.lock = lock;
            this.cause = cause;
            this.thread = Thread.currentThread();
            this.reason = reason;
        }

        @SuppressWarnings("unchecked")
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
        return t.getName() + ":" + summarizeStackTrace(e);
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
