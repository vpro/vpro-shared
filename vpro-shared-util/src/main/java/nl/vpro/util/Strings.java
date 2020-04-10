package nl.vpro.util;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.Arrays;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author Michiel Meeuwissen
 * @since 1.64
 */
@Slf4j
public class Strings {

    private Strings() {
    }

    public static Stream<String> strings(String... strings) {
        return Arrays
            .stream(strings)
            .map(s -> s.split("\\s*[,\\n]\\s*"))
            .flatMap(Arrays::stream)
            .map(Strings::fromFile)
            .flatMap(s -> s)
            ;
    }

    private static Stream<String> fromFile(String s) {
        File file = new File(s);
        if (!file.canRead()) {
            return Arrays.stream(new String[]{s});
        } else {
            try {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), UTF_8));
                return reader.lines().onClose(() -> {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        log.error(e.getMessage(), e);
                    }
                });
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                return Arrays.stream(new String[]{s});
            }
        }
    }
}
