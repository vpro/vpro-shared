package nl.vpro.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author Michiel Meeuwissen
 * @since 1.64.1
 */
public class URLPathEncode {

    /**
     * Escapes every character of the input string for the path part of an URL.
     *
     * This differs from {@link URLEncoder#encode(String, String)} that it will leave more charachters untouched (see {@link #isSafe(char)}, and that space will be replaced by +;
     */
    public static String encode(String input) {
        return encode(input, true);
    }

    public static String encode(String input, boolean spaceIsPlus) {
        StringBuilder resultStr = new StringBuilder();
        for (char ch : input.toCharArray()) {
            if (ch == ' ' && spaceIsPlus) {
                resultStr.append('+');
            } else if (isSafe(ch)) {
                resultStr.append(ch);
            } else {
                try {
                    resultStr.append(URLEncoder.encode("" + ch, StandardCharsets.UTF_8.name()));
                } catch (UnsupportedEncodingException ignored) {

                }
            }
        }
        return resultStr.toString();
    }

    /**
     * Escapes every character of the input string for the path part of an URL, but only after splitting it by /.
     * Afterwards join with '/' again. This avoids that the / itself is escaped too, and this function can be used to escape all the constituents of a path seperately.
     *
     */
    public static String encodePath(String input) {
        return Arrays.stream(
            input.split("/", -1))
            .map(URLPathEncode::encode)
            .collect(Collectors.joining("/"));
    }


    private static boolean isSafe(char ch) {
        return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || // ALPHA
            (ch >= '0' && ch <= '9') || // DIGIT
            ch == '-' || ch == '.'  || ch == '_' || ch ==  '~'  || // unreserved
            ch == ':' || ch == '@'; // pchar

    }

}
