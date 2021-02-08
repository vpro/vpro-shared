package nl.vpro.guice;

import java.time.Duration;
import java.time.Period;

import org.junit.jupiter.api.Test;

import com.google.inject.TypeLiteral;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 */
class TemporalAmountConvertorTest {

    @Test
    void convert() {
        TemporalAmountConvertor convertor = new TemporalAmountConvertor();

        assertThat(convertor.convert(null, TypeLiteral.get(Period.class))).isNull();
        assertThat(convertor.convert("P2D", TypeLiteral.get(Period.class))).isEqualTo(Period.ofDays(2));

        assertThat(convertor.convert("PT1H", TypeLiteral.get(Duration.class))).isNull();
    }
}
