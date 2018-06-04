package nl.vpro.util;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 *
 * @author Michiel Meeuwissen
 * @since 0.47
 */
public class MultiLanguageString {

    public static final Locale DEFAULT = new Locale("NL");

    private final Map<Locale, String> strings = new HashMap<>();


    public static MultiLanguageString of(String string) {
        MultiLanguageString s = new MultiLanguageString();
        s.strings.put(DEFAULT, string);
        return s;
    }

    public String get(Locale locale) {
        return strings.get(locale);
    }

    @Override
    public String toString() {
        return strings.get(DEFAULT);
    }
}
