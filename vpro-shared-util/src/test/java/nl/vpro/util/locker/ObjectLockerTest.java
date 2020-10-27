package nl.vpro.util.locker;

import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static nl.vpro.util.locker.ObjectLocker.withKeyLock;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;


/**
 * @author Michiel Meeuwissen
 */
@Slf4j
public class ObjectLockerTest {

    {
        ObjectLocker.setSessionFactory(mock(SessionFactory.class));
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
        ObjectLocker.maxLockAcquireTime = Duration.ofMillis(20);
        ObjectLocker.minWaitTime = Duration.ofMillis(5);
        try {
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
            // the lock took too long (over 20 ms in this), continued without lock
            assertThat(events).containsExactly("a1", "b1", "a2");

        } finally {
            ObjectLocker.monitor = false;
        }
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
    public void twoDifferentLocks() {
        ObjectLocker.stricltyOne = false;

        final List<String> events = new CopyOnWriteArrayList<>();
        withKeyLock(new Key("keya"), "test2", () -> {
            events.add("a1");
            withKeyLock(new Key("keyb"), "nested", () -> {
                events.add("b2");
            });
        });
        assertThat(events).containsExactly("a1", "b2");


    }

    @Test
    public void twoDifferentLocksStrictly() {
        ObjectLocker.stricltyOne = true;
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
