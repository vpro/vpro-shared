package nl.vpro.util;

import java.io.*;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 1.64
 */
public class StringsTest {

    @Test
    public void strings() {
        List<String> result = Strings.strings("a,b", "c,d").collect(Collectors.toList());
        assertThat(result).containsExactly("a", "b", "c", "d");
    }

    @Test
    public void stringsWithNewlines() {
        List<String> result = Strings.strings("a\n b", "c,d").collect(Collectors.toList());
        assertThat(result).containsExactly("a", "b", "c", "d");
    }

    @Test
    public void stringsFile() throws IOException {
        File tempFile = File.createTempFile("test", "test");
        tempFile.deleteOnExit();
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(tempFile), UTF_8)) {
            writer.append("f1\n");
            writer.append("f2\n");
        }
        List<String> result = Strings.strings("a,b", tempFile.toString(), "c,d").collect(Collectors.toList());
        assertThat(result).containsExactly("a", "b", "f1", "f2", "c", "d");

        assertThat(Strings.strings("a,b", tempFile.toString(), "c,d")
            .sorted().collect(Collectors.toList()))
                .containsExactly("a", "b", "c", "d", "f1", "f2");


    }

}
