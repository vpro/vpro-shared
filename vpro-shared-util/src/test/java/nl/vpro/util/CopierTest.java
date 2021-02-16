package nl.vpro.util;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

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
    public void basic() throws InterruptedException, IOException {
        try (
            InputStream in = randomStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Copier copier = new Copier(in, out)) {

            copier.execute();
            assertThatThrownBy(copier::execute).isInstanceOf(IllegalStateException.class);

            copier.waitForAndClose();

            assertThat(out.toByteArray()).hasSize(SIZE);
            assertCopierHappy(copier);
        }
    }


    @Test
    public void basicConsuming() throws InterruptedException, IOException {
        final List<String> batches = new ArrayList<>();
        try (
            InputStream in = randomStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Copier copier = Copier
                .builder()
                .input(in)
                .batch(213)
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
    public void ioExceptional(TestInfo info) throws InterruptedException {
        InputStream in = randomStream((count) ->  {
            if  (count >= SIZE / 2) {
                throw new IOException("foo bar");
            }
        });
        final Object notifiable = new Object();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Copier copier = Copier
            .builder()
            .input(in)
            .output(out)
            .batch(100)
            .notify(notifiable)
            .name(info.getDisplayName())
            .build();

        copier.executeIfNotRunning();

        synchronized (notifiable) {
            while(!copier.isReady()) {
                notifiable.wait();
                log.info("X");
            }
        }

        //assertThat(copier.getCount()).isEqualTo(500);
        assertThat(copier.isReady()).isTrue();
        assertThatThrownBy(copier::isReadyIOException).isInstanceOf(IOException.class)
            .hasMessage("foo bar");
        assertThat(copier.getException()).isPresent().containsInstanceOf(IOException.class);

    }


    @Test
    public void runtimeExceptional() throws InterruptedException {
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
            .batch(100)
            .callback((c) ->
                callback.add(c.toString())
            )
            .errorHandler((c, e) -> {
                exceptionHolder[0] = e;
            })
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
    public void errorHandlerError() throws InterruptedException {
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
    public void interrupt() throws InterruptedException, IOException {
        final List<String> batches = new CopyOnWriteArrayList<>();
        try (
            InputStream in = randomStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Copier copier = Copier
                .builder()
                .input(in)
                .batch(20)
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
    public void equalsParts() {
        assertThat(Copier.equalsParts(3)).containsExactly(3);
        assertThat(Copier.equalsParts(100)).containsExactly(100);
        assertThat(Copier.equalsParts(9000)).containsExactly(4500, 4500);
        assertThat(Copier.equalsParts(9001)).containsExactly(4501, 4500);
        assertThat(Copier.equalsParts(100000)).containsExactly(7693, 7693, 7693, 7693, 7692, 7692, 7692, 7692, 7692, 7692, 7692, 7692, 7692);

        assertThat(Arrays.stream(Copier.equalsParts(100000)).sum()).isEqualTo(100000);
    }


    private void assertCopierHappy(Copier copier) throws IOException {
        assertThat(copier.getCount()).isEqualTo(SIZE);
        assertThat(copier.isReadyIOException()).isTrue();
        assertThat(copier.getException()).isNotPresent();
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
