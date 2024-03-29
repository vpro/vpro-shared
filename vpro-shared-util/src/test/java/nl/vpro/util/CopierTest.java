package nl.vpro.util;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static nl.vpro.util.Copier.DEFAULT_BATCH_SIZE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Michiel Meeuwissen
 * @since 2.18
 */
@Slf4j
class CopierTest {

    private static final int SIZE = 1000;

    @Test
    void basic() throws InterruptedException, IOException {
        try (
            final InputStream in = randomStream();
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final Copier copier = new Copier(in, out)) {

            copier.execute();
            assertThatThrownBy(copier::execute).isInstanceOf(IllegalStateException.class);

            copier.waitForAndClose();

            assertThat(out.toByteArray()).hasSize(SIZE);
            assertCopierHappy(copier);
        }
    }


    @Test
    void basicConsuming() throws InterruptedException, IOException {
        final List<String> batches = new ArrayList<>();
        try (
            final InputStream in = randomStream();
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final Copier copier = Copier
                .builder()
                .input(in)
                .batch(213L)
                .batchConsumer((c) ->
                    batches.add("" + c.getCount())
                )
                .notify(batches)
                .output(out)
                .build()) {

            copier.execute();
            synchronized (batches) {
                while(!copier.isReady()) {
                    batches.wait();
                    log.info("{}", batches);
                }
            }

            assertThat(out.toByteArray()).hasSize(SIZE);
            assertCopierHappy(copier);
            assertThat(batches).containsExactly("213", "426", "639", "852", "1000");
        }
    }


    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    @Test
    void ioExceptional(TestInfo info) throws InterruptedException, IOException {
        InputStream in = randomStream((count) ->  {
            if (count % 10 ==0) {
                log.info("" + count);
            }
            if (count >= SIZE / 2) {
                log.info("Causing exception now at " + count);
                throw new IOException("foo bar");
            }
        });
        final Object notifiable = new Object();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
        Copier copier = Copier
            .builder()
            .input(in)
            .output(out)
            .batch(100L)
            .notify(notifiable)
            .name(info.getDisplayName())
            .build()) {

            if (copier.executeIfNotRunning()) {
                log.info("Started copier");
            } else {
                log.info("Copier was running already");
            }

            synchronized (notifiable) {
                while (!copier.isReady()) {
                    notifiable.wait();
                    log.info("Waited");
                }
            }
            log.info("Ready");
            //assertThat(copier.getCount()).isEqualTo(500);
            assertThat(copier.isReady()).isTrue();
            assertThatThrownBy(copier::isReadyIOException).isInstanceOf(IOException.class)
                .hasMessage("foo bar");
            assertThat(copier.getException()).isPresent().containsInstanceOf(IOException.class);
        }
    }


    @Test
    void runtimeExceptional() throws InterruptedException {
        InputStream in = randomStream((count) ->  {
            if  (count == 500) {
                throw new RuntimeException("foo bar");
            }
        });

        final List<String> callback = new ArrayList<>();
        final Throwable[] exceptionHolder = new Throwable[1];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Copier copier = Copier
            .builder()
            .input(in)
            .output(out)
            .batch(100L)
            .callback((c) ->
                callback.add(c.toString())
            )
            .errorHandler((c, e) ->
                exceptionHolder[0] = e
            )
            .build()) {

            copier.waitFor();

            assertThat(copier.isReady()).isTrue();
            assertThatThrownBy(copier::isReadyIOException).isInstanceOf(IOException.class)
                .hasMessage("java.lang.RuntimeException: foo bar");
            assertThat(copier.getException()).isPresent().containsInstanceOf(RuntimeException.class);
            assertThat(callback).hasSize(1);
            assertThat(exceptionHolder[0]).isEqualTo(copier.getException().get());
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

    }

    @Test
    void errorHandlerError() throws InterruptedException {
        InputStream in = randomStream((count) ->  {
            if  (count == 500) {
                throw new RuntimeException("foo bar");
            }
        });

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Copier copier = Copier
            .builder()
            .input(in)
            .output(out)
            .errorHandler((c, e) -> {
                throw new NullPointerException("bug!");
            })
            .build();

        copier.waitFor();

        assertThat(copier.isReady()).isTrue();
        assertThatThrownBy(copier::isReadyIOException).isInstanceOf(IOException.class)
            .hasMessage("java.lang.RuntimeException: foo bar");
        assertThat(copier.getException()).isPresent().containsInstanceOf(RuntimeException.class);


    }

    @Test
    void interrupt() throws InterruptedException, IOException {
        final List<String> batches = new CopyOnWriteArrayList<>();
        try (
            InputStream in = randomStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Copier copier = Copier
                .builder()
                .input(in)
                .batch(20L)
                .output(out)
                .batchConsumer((c) -> {
                    sleep(10);
                    batches.add("" + c.getCount());
                })
                .notify(batches)
                .build()) {
            assertThat(copier.cancelFutureIfNeeded()).isFalse();

            copier.execute();

            synchronized (batches) {
                boolean interrupted = false;
                while(!copier.isReady()) {
                    batches.wait();
                    if (batches.size() >= 2) {
                        assertThat(copier.cancelFutureIfNeeded()).isEqualTo(! interrupted);
                        interrupted = true;
                    }
                    log.info("{}", batches);
                }
            }

            assertThat(batches).startsWith("20", "40");
            assertThat(copier.isReady()).isTrue();
            assertThat(copier.cancelFutureIfNeeded()).isFalse();
        }
    }

    @Test
    void equalsParts() {
        assertThat(Copier.equalsParts(3, DEFAULT_BATCH_SIZE)).containsExactly(3);
        assertThat(Copier.equalsParts(100, DEFAULT_BATCH_SIZE)).containsExactly(100);
        assertThat(Copier.equalsParts(9000, DEFAULT_BATCH_SIZE)).containsExactly(4500, 4500);
        assertThat(Copier.equalsParts(9001, DEFAULT_BATCH_SIZE)).containsExactly(4501, 4500);
        assertThat(Copier.equalsParts(100000, DEFAULT_BATCH_SIZE)).containsExactly(7693, 7693, 7693, 7693, 7692, 7692, 7692, 7692, 7692, 7692, 7692, 7692, 7692);

        assertThat(Copier.equalsParts(12, 1)).containsExactly(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1);


        assertThat(Arrays.stream(Copier.equalsParts(100000, 8172)).sum()).isEqualTo(100000);
    }


    private void assertCopierHappy(Copier copier) throws IOException {
        assertThat(copier.getCount()).isEqualTo(SIZE);
        assertThat(copier.isReadyIOException()).isTrue();
        assertThat(copier.getException()).isNotPresent();
        copier.close();
        assertThat(copier.cancelFutureIfNeeded()).isFalse(); // should be done
    }

    private  InputStream randomStream() {
        return  randomStream((i) -> {});
    }

    private  InputStream randomStream(final CountConsumer check) {
        return new InputStream() {
            int count = 0;
            final Random random = new Random(0);
            @Override
            public int read() throws IOException {
                check.accept(count);
                if (count++ < SIZE) {

                    return random.nextInt(255);
                }

                return -1;
            }
        };
    }

    @SneakyThrows
    private void sleep(long millis) {
        Thread.sleep(millis);
    }

    @FunctionalInterface
    interface CountConsumer {
        void accept(int value) throws IOException;
    }

}
