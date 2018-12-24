package nl.vpro.util;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Michiel Meeuwissen
 * @since 2.3
 */
@XmlJavaTypeAdapter(IntegerVersion.Adapter.class)
public class IntegerVersion extends Version<Integer> {

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
        String[] split = string.split("\\.");
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

}