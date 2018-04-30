package nl.vpro.logging.simple;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.time.Instant;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.event.Level;

/**
 * Simply logs everything to a File.
 * @author Michiel Meeuwissen
 * @since 1.77
 */
@Slf4j
public class FileSimpleLogger implements SimpleLogger<FileSimpleLogger> {

    @Getter
    final File file;

    private Level level = Level.INFO;


    public static class Builder {

        public Builder tempFile(String name, String suffix) {
            try {
                return file(File.createTempFile(name, suffix));
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                throw new RuntimeException(e);
            }
        }
    }

    @lombok.Builder(builderClassName = "Builder")
    private FileSimpleLogger(
        File file,
        Level level) {
        this.file = file;
        this.level = level == null ? Level.INFO : level;
    }

    public FileSimpleLogger() {
        this(null, null);
    }

    @Override
    public void accept(Level level, String message, Throwable t) {
        if (level.toInt() < this.level.toInt()) {
            return;
        }
        try(FileWriter writer = new FileWriter(file, true);
            BufferedWriter bw = new BufferedWriter(writer);
            PrintWriter out = new PrintWriter(bw))  {
            out.println(Instant.now() + "\t" + level.name() + "\t" + message);
            if (t != null) {
                String stackTrace = ExceptionUtils.getStackTrace(t);
                out.println(stackTrace);
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public String toString() {
        return String.valueOf(file);
    }
}
