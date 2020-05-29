package nl.vpro.util;

import java.util.regex.Pattern;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.commons.lang3.StringUtils;

/**
 * A {@link Version} based on integers, basicly representing a string with a number of integers separated by a dot.
 * @author Michiel Meeuwissen
 * @since 2.3
 */
@XmlJavaTypeAdapter(IntegerVersion.Adapter.class)
public class IntegerVersion extends Version<Integer> {

    private static final Pattern SPECIAL_REGEX_CHARS = Pattern.compile("[{}()\\[\\].+*?^$\\\\|]");

    private static final String ESCAPED_SEPARATOR = escapeSpecialRegexChars(SEPARATOR);

    public IntegerVersion(String s) {
        this(_parseIntegers(s));
    }
    public IntegerVersion(Integer... parts) {
        super(parts);
    }

    public float toFloat() {
        double result = 0;
        int divider = 1;
        for (Integer p : parts) {
            result += p.doubleValue() / divider;
            divider *= 1000;
        }
        return (float) result;
    }


    protected static Integer[] _parseIntegers(String string) {
        String[] split = string.split(ESCAPED_SEPARATOR);
        Integer[] integers = new Integer[split.length];
        for (int i = 0; i < split.length; i++) {
            integers[i] = Integer.parseInt(split[i].trim());
        }
        return integers;
    }

    public static class Adapter extends XmlAdapter<String, IntegerVersion> {


        @Override
        public IntegerVersion unmarshal(String v) {
            return StringUtils.isBlank(v) ? null : new IntegerVersion(v);

        }

        @Override
        public String marshal(IntegerVersion v) {
            return v == null ? null : v.toString();

        }
    }



    /**
     * Seems like a usefull method in general, but fow now only used to escape '.'....
     */
    protected static String escapeSpecialRegexChars(String str) {

        return SPECIAL_REGEX_CHARS.matcher(str).replaceAll("\\\\$0");
    }


}
