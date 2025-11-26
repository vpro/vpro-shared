package nl.vpro.i18n;

import java.util.*;
import java.util.stream.Stream;

import org.meeuw.i18n.countries.Country;
import org.meeuw.i18n.countries.codes.CountryCode;
import org.meeuw.i18n.languages.ISO_639;
import org.meeuw.i18n.languages.ISO_639_Code;
import org.meeuw.i18n.regions.Region;

import static org.meeuw.i18n.countries.codes.CountryCode.*;
import static org.meeuw.i18n.languages.ISO_639_1_Code.ar;
import static org.meeuw.i18n.languages.ISO_639_1_Code.nl;

/**
 * @author Michiel Meeuwissen
 * @since 2.8
 */
public final class Locales {

    private Locales() {

    }

    /**
     * The locale representing the Dutch language, leaving unspecified for wich country.
     * <p>
     * Note that it seems to be undefined what {@code WeekFields.of(Locales.DUTCH).getFirstDayOfWeek()} should be. I'd day that it should be {@link java.time.DayOfWeek#MONDAY}, was it did in java 8, but in java 17 it seems to result {@link java.time.DayOfWeek#SUNDAY}, which is a bit silly, because afaik in no Dutch-speaking area in the world that would be correct.
     * <p>
     * Use {@link #NETHERLANDISH} or {@link #FLEMISH}, {@link #DUTCH_U} if it is important that the first day of the week would be monday.
     */
    public static final Locale DUTCH         = of(nl);

    /**
     * Like {@link #DUTCH}, but the country is explicitly {@link CountryCode#UNDEFINED}.
     * @since 2.34
     */
    public static final Locale DUTCH_U         = of (nl, UNDEFINED);

    /**
     * The locale representing the <a href="https://en.wikipedia.org/wiki/Arabic">Arabic language</a>, leaving unspecified for wich country.
     */
    public static final Locale ARABIC        = of(ar);

    /**
     * Dutch as spoken in the <a href="https://en.wikipedia.org/wiki/Netherlands">Netherlands</a>.
     */
    public static final Locale NETHERLANDISH = of(nl, NL);

    /**
     * Dutch as spoken in <a href="https://en.wikipedia.org/wiki/Flemish_Community">Flanders</a>.
     */
    public static final Locale FLEMISH       = of(nl, BE);

    /**
     * The locale representing the 'undetermined' language {@link ISO_639#UND}
     */
    public static final Locale UNDETERMINED  = of(ISO_639.iso639("und"));

    private static final ThreadLocal<Locale> DEFAULT = ThreadLocal.withInitial(Locale::getDefault);

    /**
     * Returns the current default locale for this thread.
     * <p>
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

    public static Locale of(ISO_639_Code lc, Country  code) {
        return new Locale(lc.code(), code.getCode());
    }

    public static Locale of(ISO_639_Code lc, CountryCode code) {
        return of(lc, Country.of(code));
    }

    public static Locale of(ISO_639_Code lc) {
        return new Locale(lc.code());
    }

    public static Locale ofString(String s) {
        return s == null ? null : Locale.forLanguageTag(s.replace('_', '-'));
    }

    public static String getCountryName(Region country, Locale locale) {
        return country.getName(locale);
    }

    /**
     * Calculates a match score between to locale objects.
     *
     * @return an integers from 0 to 3, indicating how many of the locale components match
     */
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
        if (countryScoreEquals(locale1.getCountry(), locale2.getCountry())) {
            score++;
        } else {
            return score;
        }
        if (Objects.equals(locale1.getVariant(), locale2.getVariant())) {
            score++;
        }
        return score;
    }

    private static boolean countryScoreEquals(String country1, String country2) {
        if ("UNDEFINED".equals(country1)) {
            country1 = "";
        }
        if ("UNDEFINED".equals(country2)) {
            country2 = "";
        }
        return Objects.equals(country1, country2);
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

    public static boolean simplifyable(Locale locale){
        return locale != null && (isNotEmpty(locale.getVariant()) || isNotEmpty(locale.getCountry()));
    }

    public static  Locale simplify(Locale locale) {
        if (locale == null) {
            return null;
        }
        String variant = locale.getVariant();
        if (isNotEmpty(variant)) {
            return new Locale(locale.getLanguage(), locale.getCountry());
        }
        String country = locale.getCountry();
        if (isNotEmpty(country)) {
            return new Locale(locale.getLanguage());
        }
        return locale;
    }

    private static boolean isNotEmpty(String s){
        return s != null && !s.isEmpty();
    }

}
