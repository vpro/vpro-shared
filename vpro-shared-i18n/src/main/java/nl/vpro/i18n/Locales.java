package nl.vpro.i18n;

import java.util.*;
import java.util.stream.Stream;

import org.meeuw.i18n.Region;

import com.neovisionaries.i18n.CountryCode;
import com.neovisionaries.i18n.LanguageCode;

/**
 * @author Michiel Meeuwissen
 * @since 2.8
 */
public class Locales {

    public static Locale DUTCH         = of(LanguageCode.nl);
    public static Locale ARABIC        = of(LanguageCode.ar);
    public static Locale NETHERLANDISH = of(LanguageCode.nl, CountryCode.NL);
    public static Locale FLEMISH       = of(LanguageCode.nl, CountryCode.BE);


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

    public static int score(Locale locale1, Locale locale2) {
        int score = 0;
        if (locale1 == null || locale2 == null) {
            return score;
        }
        if (Objects.equals(locale1.getLanguage(), locale2.getLanguage())) {
            score++;
        } else {
            return score;
        }
        if (Objects.equals(locale1.getCountry(), locale2.getCountry())) {
            score++;
        } else {
            return score;
        }
        if (Objects.equals(locale1.getVariant(), locale2.getVariant())) {
            score++;
        }
        return score;
    }

    public static Optional<Locale> findBestMatch(Locale request, Stream<Locale> candidates) {
        Locale locale = null;
        int score = 0;
        Iterator<Locale> iterator = candidates.iterator();
        while(iterator.hasNext()) {
            Locale candidate = iterator.next();
            int scoreOfCandidate = score(request, candidate);
            if (scoreOfCandidate > score) {
                locale = candidate;
                score = scoreOfCandidate;
            }
        }
        return Optional.ofNullable(locale);
    }


    public static  Locale simplify(Locale locale) {
        String variant = locale.getVariant();
        if (variant != null && variant.length() > 0) {
            return new Locale(locale.getLanguage(), locale.getCountry());
        }
        String country = locale.getCountry();
        if (country != null && country.length() > 0) {
            return new Locale(locale.getLanguage());
        }
        return locale;

    }

}
