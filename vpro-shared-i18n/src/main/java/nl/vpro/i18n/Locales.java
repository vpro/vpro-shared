package nl.vpro.i18n;

import java.util.*;
import java.util.stream.Stream;

import org.meeuw.i18n.regions.Region;
import org.meeuw.i18n.countries.Country;

import com.neovisionaries.i18n.*;

import static com.neovisionaries.i18n.CountryCode.BE;
import static com.neovisionaries.i18n.CountryCode.NL;

/**
 * @author Michiel Meeuwissen
 * @since 2.8
 */
public class Locales {

    /**
     * The locale representing the dutch language, leaving unspecified for wich country.
     */
    public static final Locale DUTCH         = of(LanguageCode.nl);
    /**
     * The locale representing the <a href="https://en.wikipedia.org/wiki/Arabic">arabic language</a>, leaving unspecified for wich country.
     */
    public static final Locale ARABIC        = of(LanguageCode.ar);

    /**
     * Dutch as spoken in the Netherlands.
     */
    public static final Locale NETHERLANDISH = of(LanguageCode.nl, Country.of(NL));

    /**
     * Dutch as spoken in the Flanders
     */
    public static final Locale FLEMISH       = of(LanguageCode.nl, Country.of(BE));

    /**
     * The locale representing and 'undetermined' language {@link LanguageAlpha3Code#und}
     */
    public static final Locale UNDETERMINED  = of(LanguageAlpha3Code.und);

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

    public static RestoreDefaultLocale with(Locale locale) {
        final Locale prev = getDefault();
        setDefault(locale);
        return new RestoreDefaultLocale(prev);
    }

    public static class RestoreDefaultLocale implements AutoCloseable {
        private final Locale prev;

        public RestoreDefaultLocale(Locale prev) {
            this.prev = prev;
        }

        @Override
        public void close()  {
            setDefault(prev);
        }
    }

    public static Locale of(LanguageCode lc, Country  code) {
        return new Locale(lc.name(), code.getCode());
    }

    public static Locale of(LanguageCode lc) {
        return new Locale(lc.name());
    }


    public static Locale of(LanguageAlpha3Code lc) {
        LanguageCode alpha2 = lc.getAlpha2();
        if (alpha2 != null){
            return of(alpha2);
        } else {
            return new Locale(lc.name());
        }
    }
    public static Locale of(LanguageAlpha3Code lc, Country code) {
        LanguageCode alpha2 = lc.getAlpha2();
        if (alpha2 != null){
            return of(alpha2, code);
        } else {
            return new Locale(lc.name(), code.getCode());
        }
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
