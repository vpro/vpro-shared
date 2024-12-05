package nl.vpro.util.locker;

import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.util.AbstractMap.SimpleEntry;
import java.util.*;
import java.util.concurrent.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import nl.vpro.logging.simple.*;
import nl.vpro.util.ThreadPools;

import static nl.vpro.util.locker.ObjectLocker.LOCKED_OBJECTS;
import static nl.vpro.util.locker.ObjectLocker.withKeyLock;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/**
 * @author Michiel Meeuwissen
 */
@Log4j2
@Execution(ExecutionMode.SAME_THREAD)
public class ObjectLockerTest {

    @BeforeEach
    public void setup() {
        ObjectLockerAdmin.JMX_INSTANCE.setMaxLockAcquireTime(Duration.ofSeconds(10).toString());
        ObjectLockerAdmin.JMX_INSTANCE.setStrictlyOne(false);
        ObjectLocker.minWaitTime = Duration.ofSeconds(5);
        ObjectLocker.monitor = false;
    }


    @AfterEach
    public void checkEmpty() {
        assertThat(ObjectLocker.HOLDS.get()).isEmpty();
        assertThat(ObjectLocker.LOCKED_OBJECTS).isEmpty();
        assertThat(ObjectLockerAdmin.JMX_INSTANCE.getLocks()).isEmpty();
    }

    @Test
    public void withLock() throws InterruptedException, ExecutionException {
        final List<String> events = new CopyOnWriteArrayList<>();
        ForkJoinTask<?> submitA = submit(() -> {
            withKeyLock("key", "test1", () -> {
                events.add("a1");
                assertThat(ObjectLockerAdmin.JMX_INSTANCE.getLocks().stream().map(s -> s.substring(0, 4))).containsExactly("key:");
                assertThat(ObjectLocker.HOLDS.get().get(0)).isNotEqualTo("a");
                synchronized (events) {
                    events.notifyAll();
                }
                sleep(100);
                events.add("a2");
            });
        });
        // making sure thread has been started
        synchronized (events) {
            while(events.isEmpty()) {
                events.wait(10);
            }
        }
        withKeyLock("key", "test2", () -> {
            events.add("b1");
        });
        submitA.get();
        assertThat(events).containsExactly("a1", "a2", "b1");
    }


    @Test
    public void withNullLock() throws InterruptedException, ExecutionException {
        final List<String> events = new CopyOnWriteArrayList<>();
        ForkJoinTask<?> submitA = submit(() -> {
            withKeyLock(null, "test1", () -> {
                events.add("a1");
                synchronized (events) {
                    events.notifyAll();
                }
                sleep(100);
                events.add("a2");
            });
        });
        synchronized (events) {
            while(events.isEmpty()) {
                events.wait(10);
            }
        }
        withKeyLock(null, "test2", () -> {
            events.add("b1");
        });
        submitA.get();
        // this is effectively no lock
        assertThat(events).containsExactly("a1", "b1", "a2");
    }

    /**
     * Try locking on something different from a String too.
     */
    @EqualsAndHashCode
    static class Key implements Serializable {
        @Serial
        private static final long serialVersionUID = -1689250631089355976L;
        private final String v;

        Key(String value) {
            v = value;
        }
        @Override
        public String toString() {
            return v;
        }
    }

    /**
     * Tests MSE-4946, this used to throw {{@link IllegalMonitorStateException}}
     */
    @Test
    public void withMonitor() throws InterruptedException, ExecutionException {
        ObjectLocker.monitor = true;
        ObjectLockerAdmin.JMX_INSTANCE.setMaxLockAcquireTime(Duration.ofMillis(20).toString());
        ObjectLocker.minWaitTime = Duration.ofMillis(5);

        final List<String> events = new CopyOnWriteArrayList<>();

        // one thread doing something for key 'key'
        ForkJoinTask<?> submitA = submit(() -> {
            withKeyLock(new Key("key"), "test1", () -> {
                events.add("a1");
                synchronized (events) {
                    events.notifyAll();
                }
                sleep(200);
                events.add("a2");
            });
        });
        // wait until at least one event is received.
        synchronized (events) {
            while (events.isEmpty()) {
                events.wait(10);
            }
        }
        // Then in this (other) thread, also do something with the key
        // it should wait.
        withKeyLock(new Key("key"), "test2", () -> {
            events.add("b1");
        });
        withKeyLock(new Key("anotherkey"), "test3", () -> {

        });
        // so events should contain 'a1', 'b1', 'a2'
        submitA.get();
        // the lock took too long (over 20 ms in this), continued without lock
        assertThat(events).containsExactly("a1", "b1", "a2");
        log.info("Average duration {}", ObjectLockerAdmin.JMX_INSTANCE.getAverageLockDuration().getWindowValue());
        log.info("Average acquire duration {}", ObjectLockerAdmin.JMX_INSTANCE.getAverageLockAcquireTime().getWindowValue());
    }


    @Test
    public void twoSameLocks() {
        final List<String> events = new CopyOnWriteArrayList<>();
        withKeyLock(new Key("key"), "test2", () -> {
            events.add("b1");
            withKeyLock(new Key("key"), "nested", () -> {
                events.add("b2");
            });
        });
        assertThat(events).containsExactly("b1", "b2");


    }

    @Test
    public void twoDifferentLocksAndTestLockerAdmin() {
        ObjectLocker.strictlyOne = false;
        ObjectLockerAdmin.JMX_INSTANCE.reset();
        int before = ObjectLockerAdmin.JMX_INSTANCE.getLockCount();
        final List<String> listenedEvents = new CopyOnWriteArrayList<>();
        ObjectLocker.Listener listener = (type, holder, duration) -> listenedEvents.add(type + ":" + holder.key);
        final List<String> events = new CopyOnWriteArrayList<>();
        ObjectLocker.listen(listener);
        withKeyLock(new Key("keya"), "test2", () -> {
            events.add("a1");
            assertThat(ObjectLockerAdmin.JMX_INSTANCE.getCurrentCount()).isEqualTo(1);
            withKeyLock(new Key("keyb"), "nested", () -> {
                events.add("b2");
                assertThat(ObjectLockerAdmin.JMX_INSTANCE.getCurrentCount()).isEqualTo(2);

            });
        });
        assertThat(events).containsExactly("a1", "b2");
        assertThat(ObjectLockerAdmin.JMX_INSTANCE.getCurrentCount()).isEqualTo(0);
        assertThat(ObjectLockerAdmin.JMX_INSTANCE.getCurrentCounts()).containsOnly(new SimpleEntry<>("nested", 0), new SimpleEntry<>("test2", 0));
        assertThat(ObjectLockerAdmin.JMX_INSTANCE.getLockCount()).isEqualTo(before + 2);
        assertThat(ObjectLockerAdmin.JMX_INSTANCE.getLockCounts()).containsOnly(new SimpleEntry<>("nested", 1), new SimpleEntry<>("test2", 1));
        assertThat(ObjectLockerAdmin.JMX_INSTANCE.getMaxDepth()).isEqualTo(1);
        assertThat(ObjectLockerAdmin.JMX_INSTANCE.getMaxConcurrency()).isEqualTo(0);
        assertThat(Duration.parse(ObjectLockerAdmin.JMX_INSTANCE.getMaxLockAcquireTime())).isEqualTo(Duration.ofSeconds(10));

        assertThat(listenedEvents).containsExactly("LOCK:keya", "LOCK:keyb", "UNLOCK:keyb", "UNLOCK:keya");
        ObjectLocker.unListen(listener);
        assertThat(ObjectLocker.getLockedObjects()).isEmpty();

    }

    @Test
    public void twoDifferentLocksStrictly() {
        ObjectLockerAdmin.JMX_INSTANCE.setStrictlyOne(true);
        assertThatThrownBy(() -> {
            final List<String> events = new CopyOnWriteArrayList<>();
            withKeyLock(new Key("keya"), "test2", () -> {
                events.add("a1");
                withKeyLock(new Key("keyb"), "nested", () -> {
                    events.add("b2");
                });
            });
        }).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void delayedClose() throws InterruptedException, ExecutionException {
        //TestClock testClock = TestClock.twentyTwenty();
        //ObjectLocker.clock = testClock;
        //ObjectLocker.sleeper = d -> testClock.sleep(d.toMillis());

        List<Event> events = Collections.synchronizedList(new ArrayList<>());
        SimpleLogger logger = Log4j2SimpleLogger.of(log).chain(QueueSimpleLogger.of(events::add));

        CompletableFuture<Void> voidCompletableFuture = new CompletableFuture<>();
        try (var closer = ObjectLocker.acquireLock("bla", "initiallock", ObjectLocker.LOCKED_OBJECTS, Duration.ofSeconds(5))) {
            ThreadPools.backgroundExecutor.execute(() -> {
                logger.info("Concurrent: waiting for lock");
                ObjectLocker.withKeyLock("bla", "concurrent lock", () -> {
                    logger.info("Concurrent: acquired lock");
                    sleep(100);
                    logger.info("Concurrent: about to release lock");
                });
                logger.info("Concurrent released lock. Completing proceess now.");
                voidCompletableFuture.complete(null);

            });
            sleep(100);
            logger.info("DelayClosing");
        }

        logger.info("going ahead..");
        voidCompletableFuture.get();
        logger.info("ready");
        assertThat(LOCKED_OBJECTS).isEmpty();
        assertThat(events.stream().map(Event::getMessage)).containsExactly(
            "Concurrent: waiting for lock",
            "DelayClosing",
            "going ahead..",
            "Concurrent: acquired lock",
            "Concurrent: about to release lock",
            "Concurrent released lock. Completing proceess now.",
            "ready"
        );
    }


    @Test
    public void delayedClose2() throws InterruptedException, ExecutionException {
        List<Event> events = Collections.synchronizedList(new ArrayList<>());
        SimpleLogger logger = Log4j2SimpleLogger.of(log).chain(QueueSimpleLogger.of(events::add));
        CompletableFuture<Void> voidCompletableFuture = new CompletableFuture<>();
        try (var closer = ObjectLocker.acquireLock("bla", "initiallock", ObjectLocker.LOCKED_OBJECTS, Duration.ofSeconds(10))) {
            Thread.sleep(100);
            logger.info("DelayClosing");
        }

        logger.info("going ahead..");
        ObjectLocker.withKeyLock("bla", "concurrent lock", () -> {
            logger.info("Concurrent: acquired lock");
            sleep(100);
            logger.info("Concurrent: about to release lock");
        });
        logger.info("Concurrent released lock. Completing proceess now.");
        voidCompletableFuture.complete(null);

        voidCompletableFuture.get();
        logger.info("ready");
        assertThat(LOCKED_OBJECTS).isEmpty();
        assertThat(events.stream().map(Event::getMessage)).containsExactly(
            "DelayClosing",
            "going ahead..",
            "Concurrent: acquired lock",
            "Concurrent: about to release lock",
            "Concurrent released lock. Completing proceess now.",
            "ready"
        );

    }



    @SneakyThrows
    private static void sleep(long duration) {
        ObjectLocker.sleeper.accept(Duration.ofMillis(duration));
    }

    static  ForkJoinTask<?>  submit(Runnable runnable) {
        return ForkJoinPool.commonPool().submit(() -> {
            runnable.run();
            log.info("Ready with thread {}", Thread.currentThread().getName());
            assertThat(ObjectLocker.HOLDS.get()).isEmpty();
        });
    }

}
