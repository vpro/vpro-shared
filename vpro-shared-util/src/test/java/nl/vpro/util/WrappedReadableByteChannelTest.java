package nl.vpro.util;

import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Random;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Log4j2
class WrappedReadableByteChannelTest {

    @Test
    public void test() throws IOException {
        Random random = new Random();
        int size = random.nextInt(1_000_000) + 500_000;
        ReadableByteChannel channel = Channels.newChannel(new RandomStream(random, size));
        try (WrappedReadableByteChannel wrapped = WrappedReadableByteChannel.builder()
            .delegate(channel)
            .batchSize(100_000L)
            .consumer((l) -> log.info("{}", () -> FileSizeFormatter.DEFAULT.format(l)))
            .build()) {
            IOUtils.copy(Channels.newInputStream(wrapped), NullOutputStream.nullOutputStream());

            assertThat(wrapped.getTotal()).isEqualTo(size);
        }


    }

}
