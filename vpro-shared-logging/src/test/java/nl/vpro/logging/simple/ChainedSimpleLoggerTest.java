package nl.vpro.logging.simple;

import lombok.extern.slf4j.Slf4j;

import org.junit.Test;
import org.slf4j.event.Level;

import nl.vpro.logging.Slf4jHelper;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Michiel Meeuwissen
 * @since 1.79
 */
public class ChainedSimpleLoggerTest {

    @Test
    public void isEnabled() {
        ChainedSimpleLogger logger = new ChainedSimpleLogger(
            new SLogger(Level.DEBUG),
            new SLogger(Level.WARN));

        assertThat(logger.isEnabled(Level.TRACE)).isFalse();
        assertThat(logger.isEnabled(Level.DEBUG)).isTrue();
            }

    @Slf4j
    static class SLogger implements SimpleLogger {
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
        }
    }
}
