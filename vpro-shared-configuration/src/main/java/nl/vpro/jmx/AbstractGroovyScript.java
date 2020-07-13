package nl.vpro.jmx;

import lombok.Getter;

import java.io.File;
import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.context.ApplicationContext;

import nl.vpro.logging.simple.*;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
public abstract class AbstractGroovyScript {

    protected final SimpleLogger logger;
    protected final ApplicationContext applicationContext;

    protected boolean interrupted = false;

    public AbstractGroovyScript(ApplicationContext applicationContext, SimpleLogger logger) {
        final Logger log  = LoggerFactory.getLogger(getClass());
        this.logger = ChainedSimpleLogger.of(Slf4jSimpleLogger.of(log),
            FileSimpleLogger.builder()
                .tempFile(getClass().getSimpleName() + "." + Instant.now(), ".log")
                .level(Level.DEBUG)
                .build(), logger);
        log.info("Logging to {}", this.logger);
        this.applicationContext = applicationContext;
    }



    @lombok.Builder
    @Getter
    public static class FileAndMeta {
        final File file;
        final Long count;
        final Duration duration;

        @Override
        public String toString() {
            return String.valueOf(file) + " (" + count + " lines)";
        }
    }

    abstract void run(String... args);


}
