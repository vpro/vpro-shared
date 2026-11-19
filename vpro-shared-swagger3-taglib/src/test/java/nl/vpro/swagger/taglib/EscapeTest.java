package nl.vpro.swagger.taglib;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EscapeTest {

    @Test
    void javascript() {
        assertThat(Escape.javascript(null)).isNull();
        assertThat(Escape.javascript("alpha")).isEqualTo("alpha");
        assertThat(Escape.javascript("a'b\"c\\d")).isEqualTo("a\\'b\\\"c\\\\d");
        assertThat(Escape.javascript("</script>&\n")).isEqualTo("\\u003C/script\\u003E\\u0026\\n");
        assertThat(Escape.javascript("\t\u2028")).isEqualTo("\\u0009\\u2028");
    }
}
