package nl.vpro.i18n;

import lombok.Getter;
import lombok.Setter;

import java.text.MessageFormat;
import java.util.*;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import static java.util.Locale.ENGLISH;
import static nl.vpro.i18n.Locales.DUTCH;

/**
 * Represent multiple strings for different locales.
 * @author Michiel Meeuwissen
 * @since 0.47
 */
public class MultiLanguageString implements CharSequence {


    private final Map<Locale, String> strings = new HashMap<>();

    @Getter
    @Setter
    private Locale defaultLocale = null;


    private Object[] slf4jArgs;

    private Object[] args;


    public static Builder.In in(Locale locale) {
        Builder builder = new Builder();
        return builder.defaultLocale(locale).in(locale);
    }

    public static Builder of(Locale locale, String text) {
        return in(locale).is(text);
    }

    public static Builder en(String text) {
        Builder builder = new Builder();
        return builder.en(text);
    }
    public static Builder builder() {
        return new Builder();
    }

    public static String get(CharSequence multiLanguageString, Locale locale) {
        if (multiLanguageString instanceof MultiLanguageString){
            return ((MultiLanguageString) multiLanguageString).get(locale);
        } else {
            return multiLanguageString.toString();
        }
    }

    public String get(Locale locale) {
        LocalizedString localized = getLocalized(locale);
        if (localized != null) {
            return localized.getValue();
        } else {
            return null;
        }
    }

    public LocalizedString getLocalized(Locale locale) {
        String result = strings.get(locale);
        while (result == null && Locales.simplifyable(locale)) {
            locale = Locales.simplify(locale);
            result = strings.get(locale);
        }
        if (result == null) {
            result = strings.get(defaultLocale);
        }
        if (args != null && result != null) {
            MessageFormat ft = new MessageFormat(result);
            ft.setLocale(locale);
            result = ft.format(args);
        }
        if (slf4jArgs != null && result != null) {
            FormattingTuple ft = MessageFormatter.arrayFormat(result, slf4jArgs);
            result = ft.getMessage();
        }
        return LocalizedString.of(result, locale);

    }

    @Override
    public int length() {
        return toString().length();

    }

    @Override
    public char charAt(int index) {
        return toString().charAt(index);

    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return toString().subSequence(start, end);

    }

    @Override
    @NonNull
    public String toString() {
        String s = get(defaultLocale == null ? Locales.getDefault() : defaultLocale);
        if (s == null) {
            return "";
        }
        return s;
    }

    public static class Builder {
        MultiLanguageString created = new MultiLanguageString();

        public Builder defaultLocale(Locale locale) {
            created.defaultLocale = locale;
            return this;
        }


        public Builder slf4jArgs(Object... args) {
            created.slf4jArgs = args;
            return this;
        }

         public Builder args(Object... args) {
            created.args = args;
            return this;
        }

        public Builder nl(String text) {
            created.strings.put(DUTCH, text);
            return this;
        }
        public Builder en(String text) {
            created.strings.put(ENGLISH, text);
            return this;
        }

        public In in(Locale locale) {
            return new In(locale);
        }

        public In in(String locale) {
            return new In(new Locale(locale));
        }
        public MultiLanguageString build() {
            return created;
        }
        public class In {
            private final Locale locale;

            public In(Locale locale) {
                this.locale = locale;
            }

            public Builder is(String string) {
                Builder.this.created.strings.put(locale, string);
                return Builder.this;
            }
        }
    }
}
