package nl.vpro.util;

import lombok.Getter;
import lombok.Setter;

import java.io.*;
import java.nio.charset.StandardCharsets;

import nl.vpro.logging.simple.Level;
import nl.vpro.logging.simple.SimpleLogger;


/**
 * A wrapper for an {@link InputStream} that logs it's first bytes.
 */
@Setter
@Getter
public class LoggingInputStream  extends TruncatedObservableInputStream {


    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private final SimpleLogger logger;
    private final Level level;

    public LoggingInputStream(SimpleLogger log, InputStream wrapped, Level level) {
        super(wrapped);
        this.logger = log;
        this.level = level;
    }

     public LoggingInputStream(SimpleLogger log, InputStream wrapped) {
        this(log, wrapped, Level.INFO);
    }

    @Override
    void write(byte[] buffer, int offset, int length) throws IOException {
        bytes.write(buffer, offset, length);
    }

    @Override
    void write(int value) throws IOException {
        bytes.write(value);

    }
      @Override
      void closed(long count, boolean truncated) {
          logger.log(level, "body of {} bytes{}:\n{}{}\n", count, truncated ? " (truncated)" : "", bytes.toString(StandardCharsets.UTF_8), truncated ? "..." : "");
      }
}
