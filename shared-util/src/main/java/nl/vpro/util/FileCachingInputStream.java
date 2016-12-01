package nl.vpro.util;

import lombok.Builder;
import lombok.Singular;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * When wrapping this around your inputstream, it will be read as fast a possible, but you can consume from it slower.
 * <p>
 * It will first buffer to an internal byte array (if the initial buffer size > 0, defaults to 2048). If that is too small it will buffer the result to a temporary file.
 * <p>
 * Use this if you want to consume an inputstream as fast as possible, while handing it at a slower pace. The cost is the creation of the temporary file.
 *
 * @author Michiel Meeuwissen
 * @since 0.50
 */

public class FileCachingInputStream extends InputStream {

    private static final int DEFAULT_INITIAL_BUFFER_SIZE = 2048;
    private static final int DEFAULT_FILE_BUFFER_SIZE = 8192;

    private static final int EOF = -1;
    private final Copier copier;
    private final byte[] buffer;
    private final int bufferLength;

    private final Path tempFile;
    private final InputStream file;
    private int count = 0;

    private Logger log = LoggerFactory.getLogger(FileCachingInputStream.class);

    @Builder
    private FileCachingInputStream(
        InputStream input,
        Path path,
        String filePrefix,
        long batchSize,
        Integer outputBuffer,
        Logger logger,
        @Singular("") List<OpenOption> openOptions,
        Integer initialBuffer,
        Boolean startImmediately
    ) throws IOException {

        super();
        if (initialBuffer == null) {
            initialBuffer = DEFAULT_INITIAL_BUFFER_SIZE;
        }
        if (initialBuffer > 0) {
            byte[] buf = new byte[initialBuffer];

            int bufferOffset = 0;
            int numRead;
            do {
                numRead = input.read(buf, bufferOffset, buf.length - bufferOffset);
                if (numRead > -1) {
                    bufferOffset += numRead;
                }
            } while (numRead != -1 && bufferOffset < buf.length);

            bufferLength = bufferOffset;

            if (bufferLength < initialBuffer) {
                buffer = buf;
                System.arraycopy(buf, 0, buffer, 0, bufferLength);
                copier = null;
                tempFile = null;
                file = null;
                return;
            } else {
                buffer = buf;
            }
        } else {
            bufferLength = 0;
            buffer = null;
        }

        tempFile = Files.createTempFile(
            path == null ? Paths.get(System.getProperty("java.io.tmpdir")) : path,
            filePrefix == null ? "file-caching-inputstream" : filePrefix,
            null);

        if (outputBuffer == null) {
            outputBuffer = DEFAULT_FILE_BUFFER_SIZE;
        }
        OutputStream out = new BufferedOutputStream(Files.newOutputStream(tempFile), outputBuffer);
        if (buffer != null) {
            out.write(buffer);
        }
        if (logger != null) {
            this.log = logger;
        }

        copier = Copier.builder()
            .input(input).offset(bufferLength)
            .output(out)
            .callback(c -> {
                IOUtils.closeQuietly(out);
                log.info("Created {} ({} bytes written)", tempFile, c.getCount());
            })
            .batch(batchSize)
            .batchConsumer(c ->
                log.info("Creating {} ({} bytes written)", tempFile, c.getCount())
            )
            .build();
        if (startImmediately == null || startImmediately) {
            copier.execute();
        }

        // https://github.com/rzwitserloot/lombok/issues/1071 ?
        if (openOptions == null) {
            openOptions = new ArrayList<>();
        }
        if (openOptions.isEmpty()) {
            openOptions.add(StandardOpenOption.DELETE_ON_CLOSE);
        }
        this.file = new BufferedInputStream(Files.newInputStream(tempFile, openOptions.stream().toArray(OpenOption[]::new)));
    }

    @Override
    public int read() throws IOException {
        if (file == null) {
            return readFromBuffer();
        } else {
            return readFromFile();
        }
    }

    @Override
    public int read(byte b[]) throws IOException {
        if (file == null) {
            return readFromBuffer(b);
        } else {
            return readFromFile(b);
        }
    }

    @Override
    public void close() throws IOException {
        if (file != null) {
            file.close();
            Files.deleteIfExists(tempFile);
        }
    }

    private int readFromBuffer() {
        if (count < bufferLength) {
            return buffer[count++];
        } else {
            return EOF;
        }
    }

    private int readFromBuffer(byte b[]) throws IOException {
        int toCopy = Math.min(b.length, bufferLength - count);
        if (toCopy > 0) {
            System.arraycopy(buffer, count, b, 0, toCopy);
            count += toCopy;
            return toCopy;
        } else {
            return EOF;
        }
    }

    private int readFromFile() throws IOException {
        copier.executeIfNotRunning();
        int result = file.read();
        while (result == EOF) {
            synchronized (copier) {
                while (!copier.isReady() && result == EOF) {
                    try {
                        copier.wait(1000);
                    } catch (InterruptedException e) {
                        log.error(e.getMessage(), e);
                    }
                    result = file.read();
                    if (copier.isReady() && count == copier.getCount()) {
                        return EOF;
                    }
                }
                if (copier.isReady() && count == copier.getCount()) {
                    return EOF;
                }
            }
        }
        count++;
        log.debug("returning {}", result);
        return result;
    }

    private int readFromFile(byte b[]) throws IOException {
        copier.executeIfNotRunning();
        if (copier.isReady() && count == copier.getCount()) {
            return EOF;
        }
        int totalresult = Math.max(file.read(b, 0, b.length), 0);

        if (totalresult < b.length) {
            synchronized (copier) {
                while (!copier.isReady() && totalresult < b.length) {
                    try {
                        copier.wait(1000);
                    } catch (InterruptedException e) {
                        log.error(e.getMessage(), e);
                    }
                    int subresult = Math.max(file.read(b, totalresult, b.length - totalresult), 0);
                    totalresult += subresult;
                    if (copier.isReady() && count + totalresult == copier.getCount()) {
                        break;
                    }
                }
            }
        }
        count += totalresult;
        log.debug("returning {}", totalresult);
        return totalresult;
    }
}
