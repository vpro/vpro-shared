package nl.vpro.util;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

class StreamInputStreamTest {


    @Test
    public void test() throws IOException {
        try (StreamInputStream<String, IOException> stream = new StreamInputStream<>(Stream.of("Hello", " ", "world!"), (s) -> s.getBytes(UTF_8))) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            IOUtils.copy(stream, out);
            assertThat(out.toString(UTF_8)).isEqualTo("Hello world!");
            assertThat(stream.getCounter().get()).isEqualTo(3);
        }


    }
}
