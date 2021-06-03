package nl.vpro.util.locker;

import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.time.Duration;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.concurrent.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static nl.vpro.util.locker.ObjectLocker.withKeyLock;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/**
 * @author Michiel Meeuwissen
 */
@Slf4j
@Execution(ExecutionMode.SAME_THREAD)
public class ObjectLockerTest {

    @BeforeEach
    public void setup() {
        ObjectLockerAdmin.JMX_INSTANCE.setMaxLockAcquireTime(Duration.ofSeconds(10).toString());
        ObjectLockerAdmin.JMX_INSTANCE.setStrictlyOne(false);
        ObjectLocker.minWaitTime = Duration.ofSeconds(5);
    }


    @AfterEach
    public void checkEmpty() {
        assertThat(ObjectLocker.HOLDS.get()).isEmpty();
        assertThat(ObjectLockerAdmin.JMX_INSTANCE.getLocks()).isEmpty();
    }

    @Test
    public void withLock() throws InterruptedException, ExecutionException {
        final List<String> events = new CopyOnWriteArrayList<>();
        ForkJoinTask<?> submitA = ForkJoinPool.commonPool().submit(() -> {
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
        ForkJoinTask<?> submitA = ForkJoinPool.commonPool().submit(() -> {
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
     * Try locking on something different than a String too..
     */
    @EqualsAndHashCode
    static class Key implements Serializable {
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
        try {
            List<String> events = basicLockTest();

            // the lock took too long (over 20 ms in this), continued without lock
            assertThat(events).containsExactly("a1", "b1", "a2");

        } finally {
            ObjectLocker.monitor = false;
        }
    }

    private List<String> basicLockTest() throws ExecutionException, InterruptedException {
        final List<String> events = new CopyOnWriteArrayList<>();

        ForkJoinTask<?> submitA = ForkJoinPool.commonPool().submit(() -> {
            withKeyLock(new Key("key"), "test1", () -> {
                events.add("a1");
                synchronized (events) {
                    events.notifyAll();
                }
                sleep(100);
                events.add("a2");
            });
        });
        synchronized (events) {
            while (events.isEmpty()) {
                events.wait(10);
            }
        }
        withKeyLock(new Key("key"), "test2", () -> {
            events.add("b1");
        });
        submitA.get();
        return events;
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
        ObjectLocker.unlisten(listener);
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



    @SneakyThrows
    private static void sleep(long duration) {
        Thread.sleep(duration);
    }


}
