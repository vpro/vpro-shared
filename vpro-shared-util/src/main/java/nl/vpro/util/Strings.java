package nl.vpro.util;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author Michiel Meeuwissen
 * @since 1.64
 * @see TextUtil
 */
@Slf4j
public class Strings {

    private Strings() {
    }

    /**
     * Creates a stream from comma- or newline-separated values, expanding readable file names into their lines.
     *
     * <p>The returned stream must be closed when it contains values read from a file.</p>
     *
     * @param strings values or readable file names to expand
     * @return a stream containing the supplied values or the lines of the referenced files
     */
    public static Stream<String> strings(String... strings) {
        return Arrays
            .stream(strings)
            .map(s -> s.split("\\s*[,\\n]\\s*"))
            .flatMap(Arrays::stream)
            .map(Strings::fromFile)
            .flatMap(s -> s)
            ;
    }

    /**
     * Returns the lines of a readable file, or the supplied value when it is not a readable file.
     *
     * @param s a possible file name
     * @return a stream of file lines, or a single-element stream containing {@code s}
     */
    private static Stream<String> fromFile(String s) {
        File file = new File(s);
        if (!file.canRead()) {
            return Arrays.stream(new String[]{s});
        } else {
            try {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(Files.newInputStream(file.toPath()), UTF_8));
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
