package nl.vpro.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class InputStreamChunkTest {


    @Test
    public void test() throws IOException {
        RandomStream stream = new RandomStream(0, 2_000_123);


        {
            InputStreamChunk chunk = new InputStreamChunk(1_000_000, stream);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            IOUtils.copy(chunk, out);
            assertThat(out.toByteArray().length).isEqualTo(1_000_000);
            assertThat(chunk.getCount()).isEqualTo(1_000_000);
        }
        {
            InputStreamChunk chunk = new InputStreamChunk(1_000_000, stream);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            IOUtils.copy(chunk, out);
            assertThat(out.toByteArray().length).isEqualTo(1_000_000);
            assertThat(chunk.getCount()).isEqualTo(1_000_000);

        }
        {
            InputStreamChunk chunk = new InputStreamChunk(1_000_000, stream);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            IOUtils.copy(chunk, out);
            assertThat(out.toByteArray().length).isEqualTo(123);
            assertThat(chunk.getCount()).isEqualTo(123);
        }


    }
}
