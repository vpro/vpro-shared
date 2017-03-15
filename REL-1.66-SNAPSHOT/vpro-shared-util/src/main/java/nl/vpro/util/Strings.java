package nl.vpro.util;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * @author Michiel Meeuwissen
 * @since 1.64
 */
@Slf4j
public class Strings {

    public static Stream<String> strings(String... strings) {
        return Arrays
            .stream(strings)
            .map(s -> s.split("\\s*,\\s*"))
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
                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
                return reader.lines();
            } catch (FileNotFoundException e) {
                log.error(e.getMessage(), e);
                return Arrays.stream(new String[]{s});
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
