package nl.vpro.util.locker;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.meeuw.functional.Predicates;
import org.meeuw.functional.ThrowingConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

/**
 * @author Michiel Meeuwissen
 */
@Slf4j
public class ObjectLocker {

    public static final Logger LOCKER_LOG = LoggerFactory.getLogger(ObjectLocker.class.getName() + ".LOCKER");

    public static Clock clock = Clock.systemUTC();
    public static ThrowingConsumer<Duration, InterruptedException> sleeper = (d) -> {
        Thread.sleep(d.toMillis());
    };


    public static BiPredicate<StackTraceElement, AtomicInteger> summaryBiPredicate =
        (e, count) -> {
            boolean match = e.getClassName().startsWith("nl.vpro") &&
                !e.getClassName().startsWith("nl.vpro.spring") &&
                e.getFileName() != null && !e.getFileName().contains("generated");
            int foundMatches;
            if (match) {
                foundMatches =  count.getAndIncrement();
            } else {
                foundMatches = count.get();
            }
            return foundMatches == 0 || match;
        };

    public static Predicate<StackTraceElement> summaryPredicate() {
        AtomicInteger matchCount = new AtomicInteger(0);
        return Predicates.withArg2(summaryBiPredicate, matchCount);
    }

    private ObjectLocker() {
        // private constructor to avoid all instantiation
    }


    /**
     * The lock(s) the current thread is holding. It would be suspicious (and a possible cause of deadlock) if that is more than one.
     */
    static final ThreadLocal<List<LockHolder<? extends Serializable>>> HOLDS = ThreadLocal.withInitial(ArrayList::new);

    /**
     * All object that are currently locked.
     * Map key -> LockHolder
     */
    static final Map<Serializable, LockHolder<Serializable>> LOCKED_OBJECTS    = new ConcurrentHashMap<>();

    public static final BiPredicate<Serializable, Serializable> CLASS_EQUALS = (o1, o2) -> Objects.equals(o1.getClass(), o2.getClass());
    static boolean strictlyOne;
    static boolean monitor;

    static ThreadLocal<Duration> threadLocalMonitorTime = ThreadLocal.withInitial(() -> null);

    static Duration maxLockAcquireTime = Duration.ofMinutes(10);

    static Duration minWaitTime  = Duration.ofSeconds(5);

    static Duration defaultWarnTime  = Duration.ofSeconds(30);

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
    public static void unListen(Listener listener){
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


    public static Map<Serializable, LockHolder<? extends Serializable>> getLockedObjects() {
        return Collections.unmodifiableMap(LOCKED_OBJECTS);
    }

    public static <T> T withKeyLock(
        Serializable id,
        @NonNull String reason,
        @NonNull Callable<T> callable) {
        return withObjectLock(id, reason, callable, ObjectLocker.LOCKED_OBJECTS, CLASS_EQUALS);
    }

    public static <T> T withKeyLock(
        Serializable id,
        @NonNull String reason,
        @NonNull Consumer<LockHolder<Serializable>> consumer,
        @NonNull Callable<T> callable) {
        return withObjectLock(
            id,
            reason,
            consumer,
            callable,
            ObjectLocker.LOCKED_OBJECTS,
            CLASS_EQUALS
        );
    }

    public static <T> T withKeyLock(
        @NonNull Serializable id,
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
     * @param comparable this determines whether two given keys are 'comparable'. If they are comparable, but different, and occurring at the same time, than this means
     *                   that some code is locking different things at the same time, which may be a cause of deadlocks. If they are difference but also not comparable, then
     *                   this remains unknown. We may e.g. be locking on a certain crid and on a mid. They are different, but it is not sure that they are actually about two different objects.
     */
    public static <T, K extends Serializable> T withObjectLock(
        @Nullable final K key,
        @NonNull final String reason,
        @NonNull final Callable<T> callable,
        @NonNull final Map<K, LockHolder<K>> locks,
        @NonNull final BiPredicate<Serializable, K> comparable) {
        return withObjectLock(key, reason, (c) -> {}, callable, locks, comparable);
    }

    @SneakyThrows
    public static <T, K extends Serializable> T withObjectLock(
        @Nullable final K key,
        @NonNull final String reason,
        @NonNull final Consumer<LockHolder<K>> consumer,
        @NonNull final Callable<T> callable,
        @NonNull final Map<K, LockHolder<K>> locks,
        @NonNull final BiPredicate<Serializable, K> comparable
    ) {
        if (key == null) {
            log.warn("Calling with null key: {}", reason);
            return callable.call();
        }
        try (final LockHolderCloser<K> lock = acquireLock(key, reason, locks, comparable, Duration.ZERO)) {
            consumer.accept(lock.lockHolder);
            return callable.call();
        }
    }

    public static <K extends Serializable> LockHolderCloser<Serializable> acquireLock(
        final Serializable key,
        final @NonNull String reason,
        final @NonNull Map<Serializable, LockHolder<Serializable>> locks,
        Duration delayAfterClose) throws InterruptedException {
        return acquireLock(key, reason, locks, CLASS_EQUALS, delayAfterClose);
    }


    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    private static  <K extends Serializable> LockHolderCloser<K> acquireLock(
        final  K key,
        final @NonNull String reason,
        final @NonNull Map<K, LockHolder<K>> locks,
        final @NonNull BiPredicate<Serializable, K> comparable,
        final Duration delayAfterRelease) throws InterruptedException {
        final long nanoStart = System.nanoTime();
        LockHolderCloser<K> closer = null;
        try {

            LockHolder<K> holder;
            boolean alreadyWaiting = false;
            while (true) {
                log.debug("Acquiring lock {} ({})", key, reason);
                synchronized (locks) {
                    holder = locks.computeIfAbsent(key, (m) ->
                        computeLock(m, reason, comparable))
                    ;
                    if (holder.disabled) {
                        // holders can (via JMX) be disabled, in which case we just dispose it now.
                        log.warn("Found a disabled lock {}. Discarding it now.", holder);
                        locks.remove(key);
                        continue;
                    }
                    closer = new LockHolderCloser<>(nanoStart, locks, holder, comparable, delayAfterRelease);
                    if (holder.lock.isLocked() && !holder.lock.isHeldByCurrentThread()) {
                        log.debug("There are already threads ({}) for {}, waiting", holder.lock.getQueueLength(), key);
                        alreadyWaiting = true;
                    }
                    break;
                }
            }


            if (Optional.ofNullable(threadLocalMonitorTime.get()).map(c -> true).orElse(monitor)) {
                monitoredLock(holder, key);
            } else {
                log.debug("Locking for {}", holder);
                holder.lock.lock();
            }

            Duration delaying = null;
            if (holder.availableAfter != null) {
                Instant now = clock.instant();
                if (holder.availableAfter.isAfter(now)) {
                    delaying = Duration.between(now, holder.availableAfter);
                    sleeper.accept(delaying);
                }
                log.debug("Acquired and waited until {}", holder.availableAfter);
                holder.availableAfter = null;
            }

            if (alreadyWaiting) {
                log.debug("Released and continuing {}", key);
            }

            log.trace("{} holdcount {}", Thread.currentThread().hashCode(), holder.lock.getHoldCount());
            final Duration acquireTime = Duration.ofNanos(System.nanoTime() - nanoStart);


            if (holder.lock.getHoldCount() == 1) {
                LOCKER_LOG.atLevel(acquireTime.compareTo(minWaitTime) > 0 ? Level.INFO : Level.DEBUG).log(
                    "Acquired lock for {} ({}) after {}{}", holder, reason, acquireTime,
                    delaying == null ? "" : (" (including a requested delay of " + delaying + ")")
                );
            }

            for (Listener listener : LISTENERS) {
                try {
                    listener.lock(holder, acquireTime);
                }  catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        } catch (InterruptedException | RuntimeException interruptedException) {
            if (closer != null) {
                closer.close();
            }
            throw interruptedException;
        }
        return closer;
    }

    private static  <K extends Serializable> void monitoredLock(LockHolder<K> holder, K key) throws InterruptedException {
        final var start = System.nanoTime();
        final var maxTime = Optional.ofNullable(threadLocalMonitorTime.get()).orElse(ObjectLocker.maxLockAcquireTime);
        Duration wait =  minWaitTime;
        final var maxWait = minWaitTime.multipliedBy(8);
        while (!holder.lock.tryLock(wait.toMillis(), TimeUnit.MILLISECONDS)) {
            final var duration = Duration.ofNanos(System.nanoTime() - start);
            log.info("Couldn't acquire lock for {} during {}, {}, locked by {}", key, duration,
                ObjectLocker.summarize(),
                holder.summarize(true)
            );
            if (duration.compareTo(maxTime) > 0) {
                log.warn("Took over {} to acquire {}, continuing without lock now", ObjectLocker.maxLockAcquireTime, holder);
                return;
            }
            if (wait.compareTo(maxWait) < 0) {
                wait = wait.multipliedBy(2);
            }
            if (holder.isDisabled()) {
                log.info("Holder got disabled, breaking now");
                break;
            }
            log.info("Now waiting {}", wait);
        }

    }

    private static <K extends Serializable>  LockHolder<K> computeLock(
        @NonNull final K key,
        @NonNull final  String reason,
        @NonNull final BiPredicate<Serializable, K> comparable) {
        log.trace("New lock for {}", key);
        List<LockHolder<? extends Serializable>> currentLocks = HOLDS.get();
        if (! currentLocks.isEmpty()) {
            final Optional<LockHolder<? extends Serializable>> compatibleLocks =
                currentLocks.stream().filter(l -> comparable.test(l.key, key)).findFirst();
            if (compatibleLocks.isPresent()) {
                if (strictlyOne) {
                    throw new IllegalStateException(String.format("%s Getting a lock on a different key! %s\n\t\t+\n%s", summarize(), compatibleLocks.get().summarize(), key));
                } else {
                    log.warn("Getting a lock on a different key! {}\n\t\t+\n{}", compatibleLocks.get().summarize(), key);
                }
            } else {
                log.debug("Getting a lock on a different (incompatible) key! {} + {}", currentLocks.get(0).key, key);
            }
        }
        final var newHolder = new LockHolder<>(key, reason, new ReentrantLock());
        HOLDS.get().add(newHolder);
        return newHolder;
    }



    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    private static  <K extends Serializable> void releaseLock(
        final long nanoStart,
        final @NonNull Map<K, LockHolder<K>> locks,
        final @NonNull LockHolder<K> lock) {
        synchronized (locks) {
            if (lock.lock.getHoldCount() == 1) {
                if (!lock.lock.hasQueuedThreads()) {
                    log.trace("Removed {}", lock.key);
                    if (lock.availableAfter == null || lock.availableAfter.isBefore(clock.instant())) {
                        locks.remove(lock.key);
                    }
                }
                final Duration duration = Duration.ofNanos(System.nanoTime() - nanoStart);
                for (Listener listener : LISTENERS) {
                    listener.unlock(lock, duration);
                }

                LOCKER_LOG.atLevel(duration.compareTo(lock.warnTime)> 0 ? Level.WARN :  Level.DEBUG).log(
                    "Released lock for {} ({}) in {}", lock.key, lock.reason, Duration.ofNanos(System.nanoTime() - nanoStart));
            }
            if (lock.lock.isHeldByCurrentThread()) { // MSE-4946
                if (lock.availableAfter == null || lock.availableAfter.isBefore(clock.instant())) {

                }
                HOLDS.get().remove(lock);
                lock.lock.unlock();

            } else {
                // can happen if 'continuing without lock'
                Thread currentThread = Thread.currentThread();
                log.warn("Current lock {} not hold by current thread {} ({}) but by {} ({})", lock, currentThread.getName(), currentThread, Optional.ofNullable(lock.thread.get()).map(Thread::getName).orElse(null), lock.thread.get(), new Exception());
            }

            locks.notifyAll();

        }
    }

    /**
     * The lock(s) the current thread is holding.
     * @since 2.34
     */
    public static List<LockHolder<? extends Serializable>> currentLocks() {
        return Collections.unmodifiableList(new ArrayList<>(HOLDS.get()));
    }


    /**
     *  Most importantly this is a wrapper around {@link ReentrantLock}, but it stores some extra meta information, like the original key, thread, and initialization time.
     * <p>
     *  It can also store the exception if that happened during the hold of the lock.
     */
    public static class LockHolder<K> {
        public final K key;
        public final ReentrantLock lock;
        final StackTraceElement[] initiator;
        final WeakReference<Thread> thread;
        final Instant createdAt = clock.instant();
        final String reason;

        @Getter
        boolean disabled = false;

        @Getter
        @Setter
        private Duration warnTime = ObjectLocker.defaultWarnTime;

        @Getter
        @Setter
        private Instant availableAfter;


        LockHolder(K k, String reason, ReentrantLock lock) {
            this.key = k;
            this.lock = lock;
            this.initiator = Thread.currentThread().getStackTrace();
            this.thread = new WeakReference<>(Thread.currentThread());
            this.reason = reason;
        }



        @SuppressWarnings("rawtypes")
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            LockHolder<?> holder = (LockHolder) o;

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
            return Duration.between(createdAt, clock.instant());
        }

        public String summarize() {
            return summarize(false);
        }

        public String summarize(boolean showThreadBusy) {
            Thread thr = this.thread.get();
            StackTraceElement[] threadStackTrace = null;
            if (thr != null && showThreadBusy) {
                threadStackTrace = thr.getStackTrace();
            }
            return key + ":" + createdAt + "(age: " + getAge() + "):" + reason + ":" +
                ObjectLocker.summarize(
                    this.thread.get(),
                    threadStackTrace == null ? this.initiator : null
                ) + (
                  threadStackTrace != null ? "\n THREAD is busy with: " + summarizeStackTrace(threadStackTrace) : ""
            );
        }

        @Override
        public String toString() {
            return "holder:" + key + ":" + createdAt + ":" + reason;
        }

        public void disable(boolean interrupt) {
            disabled = true;
            if (interrupt) {
                Thread t = thread.get();
                if (t == null){
                    log.warn("Thread of {} was collected already", this);
                } else {
                    t.interrupt();
                }
            }
        }
    }

    public static class LockHolderCloser<K extends Serializable> implements AutoCloseable {
        @Getter
        final LockHolder<K> lockHolder;

        final long nanoStart;

        final @NonNull Map<K, LockHolder<K>> locks;

        final BiPredicate<Serializable, K> comparable;
        boolean closed = false;

        final Duration delayAfterClose;

        private LockHolderCloser(
            final long nanoStart,
            @NonNull Map<K, LockHolder<K>> locks,
            @NonNull LockHolder<K> lockHolder,
            BiPredicate<Serializable, K> comparable,
            Duration delayAfterClose
            ) {
            this.nanoStart = nanoStart;
            this.locks = locks;
            this.lockHolder = lockHolder;
            this.comparable = comparable;
            this.delayAfterClose = delayAfterClose;
        }



        @Override
        public void close() {
            if (delayAfterClose.compareTo(Duration.ZERO) > 0) {
                lockHolder.setAvailableAfter(clock.instant().plus(delayAfterClose));
            }

            synchronized (locks) {
                if (!closed) {
                    releaseLock(nanoStart, locks, lockHolder);
                } else {
                    log.debug("Closed already");
                }
                closed = true;
            }
        }

        @Override
        public  String toString() {
            return lockHolder + (closed ? " (closed)" : "");
        }
    }


    private static String summarize(Thread t, StackTraceElement[] cause) {
        return Optional.ofNullable(t)
            .map(Thread::getName)
            .orElse(null) +
            (cause == null ? "": "\nCAUSE:" + summarizeStackTrace(cause));
    }

    private static String summarize() {
        return summarize(Thread.currentThread(), Thread.currentThread().getStackTrace());
    }

    private static String summarizeStackTrace(StackTraceElement[] stackTraceElements) {
        return "\n" +
            Stream.of(stackTraceElements)
                .filter(summaryPredicate())
                .map(StackTraceElement::toString)
                .collect(Collectors.joining("\n   <-"));
    }
}
