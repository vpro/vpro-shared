package nl.vpro.util;

import lombok.Builder;
import lombok.Singular;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.IOUtils;

/**
 * When wrapping this around your inputstream, it will be read as fast a possible, but you can consume from it slower.
 *
 * It will buffer the result to memory, until that becomes too large, then a file buffer will be created.
 *
 * @author Michiel Meeuwissen
 * @since 0.50
 */

@Slf4j
public class FileCachingInputStream extends PipedInputStream {

    final Copier copier;

    final Path tempFile;
    InputStream input;
    private int count = 0;

    @Builder
    private FileCachingInputStream(
        InputStream input, 
        Path path, 
        String filePrefix,
        int memoryBufferSize,
        int bufferSize) throws IOException {
        super();
        tempFile = Files.createTempFile(
            path == null ? Paths.get(System.getProperty("java.io.tmpdir")) : path, 
            filePrefix == null ? "file-caching-inputstream" : filePrefix, 
            null);
        OutputStream out = new BufferedStream(Files.newOutputStream(tempFile), bufferSize);
        copier = Copier.builder()
            .input(input)
            .output(out)
            .log(log)
            .afterReady(() -> IOUtils.closeQuietly(out))
            .batch(bufferSize)
            .build()
        ;
        ThreadPools.copyExecutor.execute(copier);
        this.input = new BufferedInputStream(Files.newInputStream(tempFile));

    }
 

    @Override
    public int read() throws IOException {
        int result = input.read();
        if (result == -1 && (count < copier.getCount() || ! copier.isReady())) {
            try {
                copier.waitFor();
                
                this.input = new BufferedInputStream(Files.newInputStream(tempFile));
            } catch (InterruptedException e) {
                throw new IOException(e);
            }
        }
        return result;

    }

    class BufferedStream extends BufferedOutputStream {

        public BufferedStream(OutputStream out, int size) {
            super(out, size);
        }

        byte[] getBuffer() {
            return buf;
        }
    }
  

}
