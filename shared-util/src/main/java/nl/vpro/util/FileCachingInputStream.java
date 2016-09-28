package nl.vpro.util;

import lombok.Builder;
import lombok.Singular;

import java.io.*;
import java.nio.file.*;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * When wrapping this around your inputstream, it will be read as fast a possible, but you can consume from it slower.
 *
 * It will buffer the result to a temporary file.
 *
 * Use this if you want to consume an inputstream as fast as possible, while handing it at a slower pace. The cost is the creation of the temporary file.
 *
 *
 * @author Michiel Meeuwissen
 * @since 0.50
 */

public class FileCachingInputStream extends InputStream {

    private static final int EOF = -1;
    private final Copier copier;
    private final byte[] buffer;
    private final Path tempFile;
    private final InputStream file;
    private int count = 0;

    private Logger log = LoggerFactory.getLogger(FileCachingInputStream.class);


    @Builder
    private FileCachingInputStream(
        InputStream input,
        Path path,
        String filePrefix,
        long  batchSize,
        int outputBuffer,
        Logger logger,
        @Singular List<OpenOption> openOptions,
        int initialBuffer
        ) throws IOException {

        super();
        int len = 0;
        if (initialBuffer > 0) {
            byte[] buf = new byte[initialBuffer];
            len = input.read(buf, 0, buf.length);

            if (len < initialBuffer) {
                buffer = new byte[len];
                copier = null;
                tempFile = null;
                file = null;
                return;
            } else {
                buffer = buf;
            }
        } else {
            buffer = null;
        }

        tempFile = Files.createTempFile(
            path == null ? Paths.get(System.getProperty("java.io.tmpdir")) : path,
            filePrefix == null ? "file-caching-inputstream" : filePrefix,
            null);

        OutputStream out = new BufferedOutputStream(Files.newOutputStream(tempFile), outputBuffer == 0 ? 8192 : outputBuffer);
        if (buffer != null) {
            out.write(buffer);
        }
        if (logger != null) {
            this.log = logger;
        }

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

        if (openOptions == null) {
            openOptions = Collections.singletonList(StandardOpenOption.DELETE_ON_CLOSE);
        }
        this.file = new BufferedInputStream(Files.newInputStream(tempFile, openOptions.stream().toArray(OpenOption[]::new)));
    }


    @Override
    public int read() throws IOException {
        if (count < buffer.length) {
            count++;
            return buffer[count];
        }
        int result = file.read();
        while (result == EOF) {
            synchronized (copier) {
                while(! copier.isReady() && result == EOF) {
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

  @Override
  public int read(byte b[]) throws IOException {
      if (copier.isReady() && count == copier.getCount()) {
          return EOF;
      }
      int totalresult = Math.max(file.read(b, 0, b.length), 0);

      if(totalresult < b.length) {
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

    @Override
    public void close() throws IOException {
        file.close();
        Files.deleteIfExists(tempFile);
    }



}
