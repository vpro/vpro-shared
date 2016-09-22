package nl.vpro.util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * When wrapping this around your inputstream, it will be read as fast a possible, but you can consume from it slower.
 *
 * It will buffer the result to memory, until that becomes to large, than a file buffer will be created.
 *
 * @author Michiel Meeuwissen
 * @since 0.50
 */
public class FileCachingInputStream extends InputStream {

    final Copier copier;

    FileInputStream input;
    int count = 0;

    public FileCachingInputStream(InputStream inputStream, Path path, String filePrefix) throws IOException {
        super();
        final Path tempFile = Files.createTempFile(path, filePrefix, null);
        OutputStream out = Files.newOutputStream(tempFile);
        copier = new Copier(inputStream, new BufferedStream(out, 8192));
        InputStream in = new BufferedInputStream(Files.newInputStream(tempFile));
        ThreadPools.copyExecutor.execute(new Copier(inputStream, new BufferedStream(out, 8192)));

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
