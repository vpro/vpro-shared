package nl.vpro.util;

import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>When wrapping this around your inputstream, it will be read as fast a possible, but you can
 * consume from it slower. </p>
 *
 * <p>It will first buffer to an internal byte array (if the initial buffer size > 0, defaults to 2048). If that is too small it will buffer the result to a temporary file.
 * </p>
 * <p>Use this if you want to consume an inputstream as fast as possible, while handing it at a
 *  slower pace. The cost is the creation of the temporary file.</p>
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

    public static class Builder {

        public Builder tempDir(URI uri) {
            return path(Paths.get(uri));
        }
        public Builder tempDir(String uri) {
            return path(Paths.get(URI.create(uri)));
        }
    }

    @lombok.Builder(builderClassName = "Builder")
    private FileCachingInputStream(
        InputStream input,
        Path path,
        String filePrefix,
        long batchSize,
        BiConsumer<FileCachingInputStream, Copier> batchConsumer,
        Integer outputBuffer,
        Logger logger,
        List<OpenOption> openOptions,
        Integer initialBuffer,
        Boolean startImmediately
    ) throws IOException {

        super();
        if(logger != null) {
            this.log = logger;
        }
        if (initialBuffer == null) {
            initialBuffer = DEFAULT_INITIAL_BUFFER_SIZE;
        }
        if (initialBuffer > 0) {
            // first use an initial buffer of memory only
            byte[] buf = new byte[initialBuffer];

            int bufferOffset = 0;
            int numRead;
            do {
                numRead = input.read(buf, bufferOffset, buf.length - bufferOffset);
                if (numRead > 0) {
                    bufferOffset += numRead;
                }
            } while (numRead != -1 && bufferOffset < buf.length);

            bufferLength = bufferOffset;

            if (bufferLength < initialBuffer) {
                // the buffer was not completely filled.
                // there will be no need for a file at all.
                buffer = buf;
                System.arraycopy(buf, 0, buffer, 0, bufferLength);

                // don't use file later on
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
        // if arriving here, a temp file will be needed
        tempFile = Files.createTempFile(
            path == null ? Paths.get(System.getProperty("java.io.tmpdir")) : path,
            filePrefix == null ? "file-caching-inputstream" : filePrefix,
            null);

        log.debug("Using {}", tempFile);
        if (outputBuffer == null) {
            outputBuffer = DEFAULT_FILE_BUFFER_SIZE;
        }
        OutputStream out = new BufferedOutputStream(Files.newOutputStream(tempFile), outputBuffer);
        if (buffer != null) {
            // write the initial buffer to the temp file too, so that this file accurately describes the entire stream
            out.write(buffer);
        }
        final BiConsumer<FileCachingInputStream, Copier> bc;
        if (batchConsumer == null) {
            bc = (t, c) ->
                log.info("Creating {} ({} bytes written)", tempFile, c.getCount());

        } else {
            bc = batchConsumer;
        }
        // The copier is responsible for copying the remaining of the stream to the file
        // in a separate thread
        copier = Copier.builder()
            .input(input).offset(bufferLength)
            .output(out)
            .callback(c -> {
                IOUtils.closeQuietly(out);
                log.info("Created {} ({} bytes written)", tempFile, c.getCount());
            })
            .batch(batchSize)
            .batchConsumer(c -> bc.accept(this, c))
            .build();
        if (startImmediately == null || startImmediately) {
            // if not started immediately, the copier will only be started if the first byte it would produce is actually needed.
            copier.execute();
        }

        if (openOptions == null) {
            openOptions = new ArrayList<>();
            openOptions.add(StandardOpenOption.DELETE_ON_CLOSE);
        }
        this.file = new BufferedInputStream(
            Files.newInputStream(tempFile, openOptions.toArray(new OpenOption[0])));
    }

    @Override
    public int read() throws IOException {
        try {
            if (file == null) {
                // the stream was small, we are reading from the memory buffer
                return readFromBuffer();
            } else {
                return readFromFile();
            }
        } catch(IOException ioe) {
            close();
            throw  ioe;
        }
    }

    @Override
    public int read(byte b[]) throws IOException {
        try {
            if (file == null) {
                return readFromBuffer(b);
            } else {
                return readFromFile(b);
            }
        } catch (IOException ioe){
            close();
            throw ioe;
        }
    }

    @Override
    public void close() throws IOException {
        if (copier != null) {
            // if somewhy close when copier is not ready yet, it can be interrupted, because we will not be using it any more.
            if (copier.interrupt()) {
                log.info("Interrupted {}", copier);
            }
        }
        if (file != null) {
            file.close();
            if (tempFile != null) {
                if (Files.deleteIfExists(tempFile)) {
                    log.debug("Deleted {}", tempFile);
                } else {
                    //   openOptions.add(StandardOpenOption.DELETE_ON_CLOSE); would have arranged that!
                    log.debug("Could not delete because didn't exists any more {}", tempFile);
                }
            }
        }
    }

    public Path getTempFile() {
        return tempFile;
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
                    log.debug("Copier not yet ready");
                    // copier is still busy, wait a second, and try again.
                    try {
                        copier.wait(1000);
                    } catch (InterruptedException e) {
                        log.error(e.getMessage(), e);
                    }
                    result = file.read();
                }
                if (copier.isReady() && result == EOF) {
                    // the copier did not return any new results
                    // don't increase count but return now.
                    return EOF;
                }
            }
        }
        //noinspection ConstantConditions
        assert result != EOF;

        count++;
        //log.debug("returning {}", result);
        return result;
    }

    private int readFromFile(byte b[]) throws IOException {
        copier.executeIfNotRunning();
        if (copier.isReady() && count == copier.getCount()) {
            return EOF;
        }
        int totalResult = Math.max(file.read(b, 0, b.length), 0);

        if (totalResult == 0) {
            synchronized (copier) {
                while (!copier.isReady() && totalResult == 0) {
                    log.debug("Copier not yet ready");
                    try {
                        copier.wait(1000);
                    } catch (InterruptedException e) {
                        log.error(e.getMessage(), e);
                    }
                    int subResult = Math.max(file.read(b, totalResult, b.length - totalResult), 0);
                    totalResult += subResult;
                }
            }
        }
        count += totalResult;
        //log.debug("returning {} bytes", totalResult);
        return totalResult;
    }
}
