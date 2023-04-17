package nl.vpro.util;

import java.io.*;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import nl.vpro.util.ExceptionUtils.ThrowingFunction;

import static org.assertj.core.api.Assertions.assertThat;

class InputStreamChunkTest {


    private static byte[] count1;
    private static byte[] count2;
    private static byte[] count3;
    public static List<ThrowingFunction<InputStreamChunk, byte[], IOException>> testers() {
        return Arrays.asList(
            chunk -> {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                IOUtils.copy(chunk, out);
                return out.toByteArray();
            },
            chunk -> {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                while (true) {
                    int b = chunk.read();
                    if (b == -1) {
                        break;
                    }
                    out.write( b);
                }
                return out.toByteArray();
            },
            chunk -> {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                while (true) {

                    byte[] bytes = new byte[8192];
                    int b = chunk.read(bytes, 0, 8192);
                    if (b == -1) {
                        break;
                    }
                    out.write(bytes, 0, b);
                }
                return out.toByteArray();
            }
        );
    }

    @ParameterizedTest
    @MethodSource("testers")
    public void test(ThrowingFunction<InputStreamChunk, byte[], IOException> tester) throws IOException {
        RandomStream stream = new RandomStream(0, 2_000_123);
        {
            InputStreamChunk chunk = new InputStreamChunk(1_000_000, stream);
            byte[] out = tester.apply(chunk);

            assertThat(out.length).isEqualTo(1_000_000);
            assertThat(chunk.getCount()).isEqualTo(1_000_000);

        }
        {
            InputStreamChunk chunk = new InputStreamChunk(1_000_000, stream);
            byte[] out = tester.apply(chunk);

            assertThat(out.length).isEqualTo(1_000_000);
            assertThat(chunk.getCount()).isEqualTo(1_000_000);


        }
        {
            InputStreamChunk chunk = new InputStreamChunk(1_000_000, stream);
            byte[] out = tester.apply(chunk);

            assertThat(out.length).isEqualTo(123);
            assertThat(chunk.getCount()).isEqualTo(123);

        }


    }


    @Test
    public void test() throws IOException {
        byte[] bytes = new byte[]{ 1 ,2, 3, 4, 5, 6, 7, 8, 9, 10};
        InputStream wrapped = new ByteArrayInputStream(bytes);
        InputStreamChunk chunk1 = new InputStreamChunk(3, wrapped);
        assertThat(chunk1.read()).isEqualTo(1);
        assertThat(chunk1.read()).isEqualTo(2);
        assertThat(chunk1.read()).isEqualTo(3);
        assertThat(chunk1.read()).isEqualTo(-1);
        InputStreamChunk chunk2 = new InputStreamChunk(3, wrapped);
        assertThat(chunk2.read()).isEqualTo(4);
        assertThat(chunk2.read()).isEqualTo(5);
        assertThat(chunk2.read()).isEqualTo(6);
        assertThat(chunk2.read()).isEqualTo(-1);
        InputStreamChunk chunk3 = new InputStreamChunk(3, wrapped);
        assertThat(chunk3.read()).isEqualTo(7);
        assertThat(chunk3.read()).isEqualTo(8);
        assertThat(chunk3.read()).isEqualTo(9);
        assertThat(chunk3.read()).isEqualTo(-1);
        assertThat(chunk3.getCount()).isEqualTo(3);

        InputStreamChunk chunk4 = new InputStreamChunk(3, wrapped);
        assertThat(chunk4.read()).isEqualTo(10);
        assertThat(chunk4.getCount()).isEqualTo(1);
        assertThat(chunk4.read()).isEqualTo(-1);
        assertThat(chunk4.getCount()).isEqualTo(1);

    }
}
