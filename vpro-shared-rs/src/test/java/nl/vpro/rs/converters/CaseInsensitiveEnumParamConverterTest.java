package nl.vpro.rs.converters;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 1.75
 */
public class CaseInsensitiveEnumParamConverterTest {


    public enum TestEnum {
        a,
        B,
        c,
        C,
        D
    }


    CaseInsensitiveEnumParamConverter<TestEnum> instance = CaseInsensitiveEnumParamConverter.getInstant(TestEnum.class);

    @Test
    public void test() {
        assertThat(instance.fromString("A")).isEqualTo(TestEnum.a);
        assertThat(instance.fromString("a")).isEqualTo(TestEnum.a);
        assertThat(instance.fromString("B")).isEqualTo(TestEnum.B);
        assertThat(instance.fromString("c")).isEqualTo(TestEnum.c);
        assertThat(instance.fromString("C")).isEqualTo(TestEnum.C);
        assertThat(instance.fromString("D")).isEqualTo(TestEnum.D);

    }

}
