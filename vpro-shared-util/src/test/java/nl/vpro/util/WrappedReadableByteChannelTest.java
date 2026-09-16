package nl.vpro.util;

import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.CountingOutputStream;
import org.junit.jupiter.api.Test;

import static java.io.OutputStream.nullOutputStream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Log4j2
class WrappedReadableByteChannelTest {

    @Test
    public void test() throws IOException {
        Random random = new Random();
        int size = random.nextInt(1_000_000) + 500_000;
        ReadableByteChannel channel = Channels.newChannel(new RandomStream(random, size));
        CountingOutputStream outputStream = new CountingOutputStream(nullOutputStream());

        try (WrappedReadableByteChannel wrapped = WrappedReadableByteChannel.builder()
            .delegate(channel)
            .batchSize(100_000L)
            .consumer((l) -> log.info("{}", () -> FileSizeFormatter.DEFAULT.format(l)))
            .build()) {
            IOUtils.copy(Channels.newInputStream(wrapped),outputStream);

            assertThat(wrapped.getTotal()).isEqualTo(size);
            assertThat(wrapped.isOpen()).isTrue();
        }
        assertThat(channel.isOpen()).isFalse();
        assertThat(outputStream.getCount()).isEqualTo(size);

    }

    @Test
    public void noConsumer() throws IOException {
        Random random = new Random();
        int size = random.nextInt(1_000_000) + 500_000;
        InputStream inputStream = new RandomStream(random, size);
        CountingOutputStream outputStream = new CountingOutputStream(nullOutputStream());
        try (WrappedReadableByteChannel wrapped = WrappedReadableByteChannel.builder()
            .inputStream(inputStream)
            .build()) {
            IOUtils.copy(Channels.newInputStream(wrapped), outputStream);

            assertThat(wrapped.getTotal()).isEqualTo(size);
            assertThat(wrapped.isOpen()).isTrue();
        }
        assertThat(outputStream.getCount()).isEqualTo(size);
    }

    @SuppressWarnings("resource")
    @Test
    public void erroneousConstruction() throws IOException {
        assertThatThrownBy(() -> WrappedReadableByteChannel.builder()
            .build()).isInstanceOf(IllegalArgumentException.class);

         assertThatThrownBy(() -> WrappedReadableByteChannel.builder()
             .inputStream(new RandomStream(new Random(), 100))
             .delegate(Channels.newChannel(new RandomStream(new Random(), 100)))
            .build()).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> WrappedReadableByteChannel.builder()
            .delegate(Channels.newChannel(InputStream.nullInputStream()))
            .batchSize(0L)
            .build()).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> WrappedReadableByteChannel.builder()
            .delegate(Channels.newChannel(InputStream.nullInputStream()))
            .batchSize(-1L)
            .build()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void consumesEveryCompletedBatchAndTheRemainderOnClose() throws IOException {
        List<Long> consumed = new ArrayList<>();
        try (WrappedReadableByteChannel wrapped = WrappedReadableByteChannel.builder()
            .inputStream(new RandomStream(new Random(), 7))
            .batchSize(3L)
            .consumer(consumed::add)
            .build()) {
            wrapped.read(ByteBuffer.allocate(7));
        }

        assertThat(consumed).containsExactly(3L, 6L, 7L);
    }

    @Test
    public void consumesTheRemainderWhenClosingFails() throws IOException {
        List<Long> consumed = new ArrayList<>();
        ReadableByteChannel delegate = new ReadableByteChannel() {
            private boolean open = true;

            @Override
            public int read(ByteBuffer dst) {
               dst.put((byte) 1);
               return 1;
            }

            @Override
            public boolean isOpen() {
               return open;
            }

            @Override
            public void close() throws IOException {
               open = false;
               throw new IOException("close failure");
            }
        };

        WrappedReadableByteChannel wrapped = WrappedReadableByteChannel.builder()
            .delegate(delegate)
            .batchSize(3L)
            .consumer(consumed::add)
            .build();
        wrapped.read(ByteBuffer.allocate(1));

        assertThatThrownBy(wrapped::close).isInstanceOf(IOException.class);
        assertThat(consumed).containsExactly(1L);
    }


}
