package nl.vpro.util;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author Michiel Meeuwissen
 * @since 1.64.1
 */
public class URLPathEncode {

    /**
     * Escapes every character of the input string for the path part of an URL.
     */
    public static String encode(String input) {
        StringBuilder resultStr = new StringBuilder();
        for (char ch : input.toCharArray()) {
            if (ch == ' ') {
                resultStr.append('+');
            } else if (isSafe(ch)) {
                resultStr.append(ch);
            } else {
                resultStr.append('%');
                resultStr.append(toHex(ch / 16));
                resultStr.append(toHex(ch % 16));
            }
        }
        return resultStr.toString();
    }

    /**
     * Escapes every character of the input string for the path part of an URL, but only after splitting it by /.
     * Afterwards join with '/' again. This avoids that the / itself is escaped too, and this function can be used to escape all the constituents of a path seperately.
     */
    public static String encodePath(String input) {
        return Arrays.stream(
            input.split("/", -1))
            .map(URLPathEncode::encode)
            .collect(Collectors.joining("/"));
    }

    private static char toHex(int ch) {
        return (char) (ch < 10 ? '0' + ch : 'A' + ch - 10);
    }

    private static boolean isSafe(char ch) {
        return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || // ALPHA
            (ch >= '0' && ch <= '9') || // DIGIT
            ch == '-' || ch == '.'  || ch == '_' || ch ==  '~'  || // unreserved
            ch == ':' || ch == '@'; // pchar

    }

}
