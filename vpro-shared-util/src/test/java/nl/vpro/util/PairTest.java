package nl.vpro.util;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 1.68
 */
public class PairTest {

    @Test
    public void test() {
        Pair<String, String> pair = Pair.<String, String>builder()
            .first("a")
            .second("b")
            .build();

        assertThat(pair.toString()).isEqualTo("(first=a, second=b)");
        assertThat(pair.getFirst()).isEqualTo("a");
        assertThat(pair.getFirstDescription()).isEqualTo("first");
        assertThat(pair.getSecond()).isEqualTo("b");
        assertThat(pair.getSecondDescription()).isEqualTo("second");


    }

    @Test
    public void testEntry() {
        Map.Entry<String, Integer> pair = Pair.<String, Integer>builder()
            .key("1")
            .value(2)
            .build();

        assertThat(pair.toString()).isEqualTo("(key=1, value=2)");
        assertThat(pair.getKey()).isEqualTo("1");
        assertThat(pair.getValue()).isEqualTo(2);
        pair.setValue(3);
        assertThat(pair.getValue()).isEqualTo(3);



    }

}
