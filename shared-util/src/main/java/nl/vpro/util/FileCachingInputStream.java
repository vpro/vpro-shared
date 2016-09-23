package nl.vpro.util;

import lombok.Builder;
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
public class FileCachingInputStream extends InputStream {

    private final Copier copier;
    private final Path tempFile;
    private InputStream file;
    private int count = 0;

    @Builder
    private FileCachingInputStream(
        InputStream input,
        Path path,
        String filePrefix,
        int batchSize,
        int outputBuffer) throws IOException {

        super();
        tempFile = Files.createTempFile(
            path == null ? Paths.get(System.getProperty("java.io.tmpdir")) : path,
            filePrefix == null ? "file-caching-inputstream" : filePrefix,
            null);
        OutputStream out = new BufferedStream(Files.newOutputStream(tempFile), outputBuffer);

        copier = Copier.builder()
            .input(input)
            .output(out)
            .callback(c -> {
                IOUtils.closeQuietly(out);
                log.info("Created {} ({} bytes written)", tempFile, c.getCount());
            })
            .batch(batchSize)
            .batchConsumer(c ->
                log.info("Creating {} ({} bytes written)", tempFile, c.getCount())
            )
            .build()
            .execute()
        ;
        this.file= new BufferedInputStream(Files.newInputStream(tempFile));

    }


    @Override
    public int read() throws IOException {
        int result = file.read();
        if (result == -1) {
            log.debug("Closing {}", tempFile);
            file.close();

            synchronized (copier) {
                while(! copier.isReady()) {
                    try {
                        copier.wait(1000);
                    } catch (InterruptedException e) {
                        log.error(e.getMessage(), e);
                    }
                    if (count < copier.getCount()) {
                        break;
                    } else {
                        return -1;
                    }
                }
            }
            if (count < copier.getCount()) {
                log.debug("Opening file {} from {}", tempFile, count);
                file = new BufferedInputStream(Files.newInputStream(tempFile));
                file.skip(count);
                result = file.read();
            } else {
                return -1;
            }
        }
        count++;
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
