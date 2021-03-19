package nl.vpro.xml.bind;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 2.24
 */
class ZeroOneBooleanAdapterTest {

    ZeroOneBooleanAdapter instance = new ZeroOneBooleanAdapter();
    @Test
    public void unmarshal() {

        assertThat(instance.unmarshal("1")).isTrue();
        assertThat(instance.unmarshal("0")).isFalse();
        assertThat(instance.unmarshal(null)).isNull();
        assertThat(instance.unmarshal("foobar")).isFalse();
        assertThat(instance.unmarshal("true")).isTrue();
    }

    @Test
    public void marshal() throws Exception {
        assertThat(instance.marshal(null)).isNull();
        assertThat(instance.marshal(true)).isEqualTo("1");
        assertThat(instance.marshal(false)).isEqualTo("0");
    }

}
