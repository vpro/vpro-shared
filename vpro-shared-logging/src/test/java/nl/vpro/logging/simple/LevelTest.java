package nl.vpro.logging.simple;

import org.junit.jupiter.api.Test;

import static nl.vpro.logging.simple.Level.WARN;
import static nl.vpro.logging.simple.Level.shiftedLevel;
import static org.assertj.core.api.Assertions.assertThat;

public class LevelTest {

    @Test
    public void shift() {
        assertThat(shiftedLevel(WARN, 0)).isEqualTo(WARN);
        assertThat(shiftedLevel(WARN, -1)).isEqualTo(Level.ERROR);
        assertThat(shiftedLevel(WARN, -3)).isEqualTo(Level.ERROR);
        assertThat(shiftedLevel(WARN, 1)).isEqualTo(Level.INFO);
        assertThat(shiftedLevel(WARN, 300)).isEqualTo(Level.TRACE);
    }
}
