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

    private static final int EOF = -1;
    private final Copier copier;
    private final Path tempFile;
    private final InputStream file;
    private int count = 0;

    @Builder
    private FileCachingInputStream(
        InputStream input,
        Path path,
        String filePrefix,
        long  batchSize,
        int outputBuffer) throws IOException {

        super();
        tempFile = Files.createTempFile(
            path == null ? Paths.get(System.getProperty("java.io.tmpdir")) : path,
            filePrefix == null ? "file-caching-inputstream" : filePrefix,
            null);

        OutputStream out = new BufferedOutputStream(Files.newOutputStream(tempFile), outputBuffer == 0 ? 8192 : outputBuffer);

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
        this.file = new BufferedInputStream(Files.newInputStream(tempFile));

    }


    @Override
    public int read() throws IOException {
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
            }

        }
        count++;
        log.debug("returing {}", result);
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
      log.debug("returing {}", totalresult);
      return totalresult;
  }

    @Override
    public void close() throws IOException {
        file.close();
        Files.deleteIfExists(tempFile);
    }



}
