package nl.vpro.jackson2;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 */
class JsonFilterTest {

    @Test
    public void test() throws IOException {
        JsonFilter.Replacement<String> replacement =
            new JsonFilter.Replacement<>("a", "b", "c");
        List<JsonFilter.Replacement<?>> replacements = Collections.singletonList(replacement);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        JsonFilter filter = new JsonFilter(new ByteArrayInputStream("{'a': 'b', 'b': 1, 'c': [], 'd': null, 'e': true, 'f': 3.14, 'g': false, 'h': {}}".getBytes(StandardCharsets.UTF_8)), outputStream, replacements);
        filter.call();
        assertThat(outputStream.toString()).isEqualTo("{\"a\":\"c\",\"b\":1,\"c\":[],\"d\":null,\"e\":true,\"f\":3.14,\"g\":false,\"h\":{}}");
    }

}
