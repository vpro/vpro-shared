package nl.vpro.util;

import org.junit.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 1.68
 */
public class PairTest {

    @Test
    public void tostring() {
        Pair<String, String> pair = Pair.<String, String>builder()
            .first("a")
            .second("b")
            .build();

        assertThat(pair.toString()).isEqualTo("(first=a, second=b)");

    }

}
