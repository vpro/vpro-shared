package nl.vpro.i18n;

import java.util.Locale;

import org.meeuw.i18n.Region;
import com.neovisionaries.i18n.CountryCode;
import com.neovisionaries.i18n.LanguageCode;

/**
 * @author Michiel Meeuwissen
 * @since 2.8
 */
public class Locales {

    public static Locale DUTCH         = of(LanguageCode.nl);
    public static Locale NETHERLANDISH = of(LanguageCode.nl, CountryCode.NL);
    public static Locale FLEMISH       = of(LanguageCode.nl, CountryCode.BE);
    public static Locale ARABIC        = of(LanguageCode.ar);


    private static final ThreadLocal<Locale> DEFAULT = ThreadLocal.withInitial(Locale::getDefault);


    /**
     * Returns the current default locale for this thread.
     *
     * This initializes with {@link Locale#getDefault()}, but can be set per thread.
     */
    public static Locale getDefault() {
        return DEFAULT.get();
    }
    public static void setDefault(Locale locale) {
        DEFAULT.set(locale);
    }
    public static void resetDefault() {
        DEFAULT.remove();
    }

    public static Locale of(LanguageCode lc, CountryCode code) {
        return new Locale(lc.name(), code.getAlpha2());
    }

    public static Locale of(LanguageCode lc) {
        return new Locale(lc.name());
    }

    public static Locale ofString(String s) {
        return s == null ? null : Locale.forLanguageTag(s.replace('_', '-'));
    }


    public static String getCountryName(Region country, Locale locale) {
        return country.getName(locale);
    }

}
