package nl.vpro.xml.bind;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EnumAdapterTest {

    @XmlEnum
    enum AnEnum {
        a,
        B,
        c,
        @XmlEnumValue("x")
        d
    }

    @Test
    public void test() {
        EnumAdapter<AnEnum> adapter = new EnumAdapter<AnEnum>(AnEnum.class) {

        };
        assertThat(adapter.valueOf("a")).isEqualTo(AnEnum.a);
        assertThat(adapter.valueOf(" a ")).isEqualTo(AnEnum.a);
        assertThat(adapter.valueOf(" A ")).isEqualTo(AnEnum.a);
        assertThat(adapter.valueOf(" x ")).isEqualTo(AnEnum.d);
        assertThat(adapter.valueOf(" d ")).isEqualTo(AnEnum.d);
    }

}
