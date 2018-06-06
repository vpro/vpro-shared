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

    public static final Locale DEFAULT = new Locale("nl");

    private final Map<Locale, String> strings = new HashMap<>();


    public static MultiLanguageString of(String string) {
        MultiLanguageString s = new MultiLanguageString();
        s.strings.put(DEFAULT, string);
        return s;
    }

    public static Builder builder() {
        return new Builder();
    }


    public String get(Locale locale) {
        return strings.get(locale);
    }

    @Override
    public String toString() {
        return strings.get(DEFAULT);
    }

    public static class Builder {
        MultiLanguageString created = new MultiLanguageString();

        public Builder nl(String text) {
            created.strings.put(new Locale("nl"), text);
            return this;
        }
        public Builder en(String text) {
            created.strings.put(Locale.ENGLISH, text);
            return this;
        }

        public Builder.In in(Locale locale) {
            return new In(locale);
        }

        public Builder.In in(String locale) {
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
