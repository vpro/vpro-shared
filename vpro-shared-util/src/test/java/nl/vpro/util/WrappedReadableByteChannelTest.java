package nl.vpro.util;

import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Random;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.CountingOutputStream;
import org.junit.jupiter.api.Test;

import static java.io.OutputStream.nullOutputStream;
import static org.assertj.core.api.Assertions.assertThat;

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
        ReadableByteChannel channel = Channels.newChannel(new RandomStream(random, size));
        CountingOutputStream outputStream = new CountingOutputStream(nullOutputStream());
        try (WrappedReadableByteChannel wrapped = WrappedReadableByteChannel.builder()
            .delegate(channel)
            .build()) {
            IOUtils.copy(Channels.newInputStream(wrapped), outputStream);

            assertThat(wrapped.getTotal()).isEqualTo(size);
            assertThat(wrapped.isOpen()).isTrue();
        }
        assertThat(channel.isOpen()).isFalse();
        assertThat(outputStream.getCount()).isEqualTo(size);
    }

}
