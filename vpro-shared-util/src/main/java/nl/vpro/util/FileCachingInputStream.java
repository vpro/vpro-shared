package nl.vpro.util;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

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
 * slower pace. The cost is the creation of the temporary file.</p>
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
    private long count = 0;

    @Getter
    private long bytesRead = 0;

    private Logger log = LoggerFactory.getLogger(FileCachingInputStream.class);

    @Slf4j
    public static class Builder {

        public Builder tempDir(URI uri) {
            return path(Paths.get(uri));
        }

        public Builder tempDir(String uri) {
            try {
                return path(Paths.get(URI.create(uri)));
            } catch (IllegalArgumentException iae) {
                log.debug("{}:{} Supposing it a file name", uri, iae.getMessage());
                return path(Paths.get(uri));
            }
        }

        public Builder tempFile(Path path) {
            return tempPath(path);
        }


        public Builder tempFile(File file) {
            return tempPath(file.toPath());
        }

        public Builder noProgressLogging() {
            return progressLogging(false);
        }

    }

    /**
     * @param batchSize Batch size
     * @param path Directory for temporary files
     */
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
        Boolean startImmediately,
        Boolean progressLogging,
        Path tempPath
    ) {

        super();
        if (logger != null) {
            this.log = logger;
        }
        if (initialBuffer == null) {
            initialBuffer = DEFAULT_INITIAL_BUFFER_SIZE;
        }
        try {
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
                    this.tempFile = null;
                    if (tempPath != null) {
                        try (FileOutputStream out = new FileOutputStream(tempPath.toFile())) {
                            IOUtils.copy(new ByteArrayInputStream(buffer), out);
                        }
                    }
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
            if (path != null) {
                if (! Files.isDirectory(path)) {
                    Files.createDirectories(path);
                    log.info("Created {}", path);
                }
            }

            this.tempFile = tempPath== null ? Files.createTempFile(
                path == null ? Paths.get(System.getProperty("java.io.tmpdir")) : path,
                filePrefix == null ? "file-caching-inputstream" : filePrefix,
                null) : tempPath;


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
                if (progressLogging == null || progressLogging) {
                    bc = (t, c) ->
                        log.info("Creating {} ({} bytes written)", tempFile, c.getCount());
                } else {
                    bc = (t, c) -> {
                    };
                }

            } else {
                if (progressLogging != null && progressLogging) {
                    bc = (t, c) -> {
                        log.info("Creating {} ({} bytes written)", tempFile, c.getCount());
                        batchConsumer.accept(t, c);
                    };
                } else {
                    bc = batchConsumer;
                }
            }

            if (openOptions == null) {
                openOptions = new ArrayList<>();
                openOptions.add(StandardOpenOption.DELETE_ON_CLOSE);
            }
            this.file = new BufferedInputStream(
                Files.newInputStream(tempFile, openOptions.toArray(new OpenOption[0])));
             // The copier is responsible for copying the remaining of the stream to the file
            // in a separate thread
            copier = Copier.builder()
                .input(input)
                .offset(bufferLength)
                .output(out)
                .name(tempFile.toString())
                .notify(this)
                .callback(c -> {
                    try {
                        out.close();
                    } catch (IOException ignore) {

                    }
                    if (progressLogging == null || progressLogging) {
                        log.info("Created {} ({} bytes written)", tempFile, c.getCount());

                    }
                })
                .batch(batchSize)
                .batchConsumer(c -> bc.accept(this, c))
                .build();
            if (startImmediately == null || startImmediately) {
                // if not started immediately, the copier will only be started if the first byte it would produce is actually needed.
                copier.execute();
            }

        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
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
        } catch (IOException ioe) {
            close();
            throw ioe;
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
        } catch (IOException ioe) {
            close();
            throw ioe;
        }
    }

    @Override
    public void close() throws IOException {
        if (file != null) {
            file.close();
        }
        if (copier != null) {
            // if somewhy close when copier is not ready yet, it can be interrupted, because we will not be using it any more.
            if (copier.interrupt()) {
                log.info("Interrupted {}", copier);
            }
        }
        if (file != null) {
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

    public synchronized long waitForBytesRead(int atLeast) throws InterruptedException {
        if (copier != null) {
            copier.executeIfNotRunning();
            while (copier.getCount() < atLeast && ! copier.isReady()) {
                wait();
            }
            return copier.getCount();
        } else {
            return bufferLength;
        }
    }

    public long getCount() {
        return copier == null ? bufferLength : copier.getCount();
    }

    public Path getTempFile() {
        return tempFile;
    }

    private int readFromBuffer() {
        if (count < bufferLength) {
            int result = buffer[(int) count++];
            bytesRead += result;
            synchronized (this) {
                notifyAll();
            }
            return result;
        } else {
            return EOF;
        }
    }

    private int readFromBuffer(byte b[]) {
        int toCopy = Math.min(b.length, bufferLength - (int) count);
        if (toCopy > 0) {
            System.arraycopy(buffer, (int) count, b, 0, toCopy);
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
                    log.debug("Copier {} not yet ready", copier.logPrefix());
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
        int totalResult = 0;
        synchronized (copier) {
            totalResult += Math.max(file.read(b, 0, b.length), 0);

            if (totalResult == 0) {

                while (!copier.isReady() && totalResult == 0) {
                    log.debug("Copier {} not yet ready", copier.logPrefix());
                    try {
                        copier.wait(1000);
                    } catch (InterruptedException e) {
                        log.error(e.getMessage(), e);
                    }
                    int subResult = Math.max(file.read(b, totalResult, b.length - totalResult), 0);
                      totalResult += subResult;
                }

            }
            count += totalResult;
        }

        //log.debug("returning {} bytes", totalResult);
        return totalResult;
    }
}
