package nl.vpro.util;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author Michiel Meeuwissen
 * @since 1.64.1
 */
public class URLPathEncode {

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
    public static String encodePath(String input) {
        return Arrays.stream(input.split("/")).map(URLPathEncode::encode).collect(Collectors.joining("/"));
    }

    private static char toHex(int ch) {
        return (char) (ch < 10 ? '0' + ch : 'A' + ch - 10);
    }

    private static boolean isSafe(char ch) {
        return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || // ALPHA
            (ch >= '0' && ch <= '0') || // DIGIT
            ch == '-' || ch == '.'  || ch == '_' || ch ==  '~'  || // unreserved
            ch == ':' || ch == '@'; // pchar

    }

}
