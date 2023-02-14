package nl.vpro.logging.simple;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import nl.vpro.logging.Slf4jHelper;
import static nl.vpro.logging.simple.ChainedSimpleLoggerTest.SLogger.logEntries;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;


/**
 * @author Michiel Meeuwissen
 * @since 1.79
 */
public class ChainedSimpleLoggerTest {

    @Test
    public void isEnabled() {
        ChainedSimpleLogger logger = new ChainedSimpleLogger(
            new SLogger(Level.DEBUG),
            new SLogger(Level.WARN),
            new SLogger(Level.ERROR).truncated(Level.WARN)
        );

        assertThat(logger.isEnabled(Level.TRACE)).isFalse();
        assertThat(logger.isEnabled(Level.DEBUG)).isTrue();
        logger.error("hoi");
        assertThat(logEntries).containsExactly(
            "ERROR\thoi",
            "ERROR\thoi",
            "WARN\thoi"
        );

    }


    @Slf4j
    static class SLogger implements SimpleLogger {
        static List<String> logEntries = new ArrayList<>();
        private final Level level;


        SLogger(Level level) {
            this.level = level;
        }

        @Override
        public boolean isEnabled(Level level) {
            return level.compareTo(this.level) <=  0;
        }

        @Override
        public void accept(Level level, CharSequence message, Throwable t) {
            Slf4jHelper.log(log, level, message.toString(), t);
            logEntries.add(level.name() + "\t" + message);
        }
    }
}
