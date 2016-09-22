package nl.vpro.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Michiel Meeuwissen
 * @since 0.50
 */
public class FileCachingInputStreamTest {
	
	@Test
	public void test() throws IOException {
	    byte[] in = new byte[] {1,2,3,4,5,6,7,8, 9};
		FileCachingInputStream inputStream  = FileCachingInputStream.builder()
            .bufferSize(2)
            .memoryBufferSize(2)
            .input(new ByteArrayInputStream(in))
            .build();
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        IOUtils.copy(inputStream, out);
        
        assertThat(out.toByteArray()).containsExactly(in);
	}

}