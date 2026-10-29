package nl.vpro.swagger.taglib;

/**
 * Escaping for values written into the generated javascript, to avoid a dependency on commons-text.
 */
public class Escape {

    /**
     * Escapes a value for use inside a quoted javascript string literal in a {@code <script>} element.
     * @return the escaped value, or {@code null} if the input is {@code null}
     */
    public static String javascript(String value) {
        if (value == null) {
            return null;
        }
        final StringBuilder builder = new StringBuilder(value.length());
        for (char c : value.toCharArray()) {
            switch (c) {
                case '\\' -> builder.append("\\\\");
                case '\'' -> builder.append("\\'");
                case '"' -> builder.append("\\\"");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                // avoid '</script>' and '<!--' in the string to terminate the script element
                case '<' -> builder.append("\\u003C");
                case '>' -> builder.append("\\u003E");
                case '&' -> builder.append("\\u0026");
                default -> {
                    if (c < 0x20 || c == ' ' || c == ' ') {
                        builder.append("\\u%04X".formatted((int) c));
                    } else {
                        builder.append(c);
                    }
                }
            }
        }
        return builder.toString();
    }
}
