package nl.vpro.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.checkerframework.checker.nullness.qual.PolyNull;

/**
 * @author Michiel Meeuwissen
 * @since 1.64.1
 */
public class URLPathEncode {

    /**
     * Escapes every character of the input string for the path part of a URL.
     *
     * This differs from {@link URLEncoder#encode(String, String)} that it will leave more characters untouched (see {@link #isSafe(char)}, and that space will be replaced by +;
     */
    @PolyNull
    public static String encode(@PolyNull String input) {
        return encode(input, true);
    }

    /**
     * Escapes every character of the input string for the path part of a URL.
     *
     * @see #encode(String)
     */
    @PolyNull
    public static String encode(@PolyNull String input, boolean spaceIsPlus) {
        if (input == null) {
            return null;
        }
        StringBuilder resultStr = new StringBuilder();
        for (char ch : input.toCharArray()) {
            if (ch == ' ') {
                if (spaceIsPlus) {
                    resultStr.append('+');
                } else {
                    resultStr.append("%20");
                }
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

    @PolyNull
    public static String decode(@PolyNull String s) {
        if (s == null) {
            return null;
        }
        try {
            return URLDecoder.decode(s, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException ignored) {
            return s;
        }
    }

    /**
     * Escapes every character of the input string for the path part of an URL, but only after splitting it by /.
     * Afterwards join with '/' again. This avoids that the / itself is escaped too, and this function can be used to escape all the constituents of a path separately.
     *
     */
    @PolyNull
    public static String encodePath(@PolyNull String input) {
        return encodePath(input, true);
    }

    @PolyNull
    public static String encodePath(@PolyNull String input, boolean spaceIsPlus) {
        if (input == null) {
            return null;
        }
        return Arrays.stream(
            input.split("/", -1))
            .map(s -> encode(s, spaceIsPlus))
            .collect(Collectors.joining("/"));
    }

    private static boolean isSafe(char ch) {
        return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || // ALPHA
            (ch >= '0' && ch <= '9') || // DIGIT
            ch == '-' || ch == '.'  || ch == '_' || ch ==  '~'  || // unreserved
            ch == ':' || ch == '@'; // pchar
    }

}
