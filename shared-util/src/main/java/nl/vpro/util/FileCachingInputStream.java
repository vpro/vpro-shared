package nl.vpro.util;

import lombok.Builder;
import lombok.Singular;
import lombok.experimental.FieldDefaults;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * When wrapping this around your inputstream, it will be read as fast a possible, but you can consume from it slower.
 *
 * It will buffer the result to memory, until that becomes to large, than a file buffer will be created.
 *
 * @author Michiel Meeuwissen
 * @since 0.50
 */

public class FileCachingInputStream extends PipedInputStream {

    final Copier copier;

    InputStream input;
    private int count = 0;
    private int bufferSize = 8192;

    @Builder
    private FileCachingInputStream(
        InputStream input, 
        Path path, 
        String filePrefix, 
        int bufferSize) throws IOException {
        super();
        final Path tempFile = Files.createTempFile(
            path == null ? Paths.get(System.getProperty("java.io.tmpdir")) : path, 
            filePrefix == null ? "file-caching-inputstream" : filePrefix, 
            null);
        OutputStream out = Files.newOutputStream(tempFile);
        copier = new Copier(input, new BufferedStream(out, bufferSize));
        copier.run();
        this.input = new BufferedInputStream(Files.newInputStream(tempFile));
        //ThreadPools.copyExecutor.execute(new Copier(input, new BufferedStream(out, bufferSize)));

    }
 

    @Override
    public int read() throws IOException {
        int result = input.read();
        if (result == -1 && (count < copier.getCount() || ! copier.isReady())) {
            
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
