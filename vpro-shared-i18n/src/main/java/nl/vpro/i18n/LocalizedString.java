package nl.vpro.i18n;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.Locale;

import javax.xml.XMLConstants;
import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.checkerframework.checker.nullness.qual.NonNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.neovisionaries.i18n.LanguageCode;

import static nl.vpro.i18n.Locales.score;

/**
 * Basicly wraps a string together with the {@link Locale} describing in what language it is.
 *
 * @author Michiel Meeuwissen
 * @since 3.2
 */
@XmlAccessorType(XmlAccessType.NONE)
@Slf4j
public class LocalizedString implements CharSequence, Serializable { //implements javax.xml.registry.infomodel.LocalizedString {

    public static LocalizedString of(String value, Locale locale) {
        if (value == null) {
            return null;
        } else {
            return LocalizedString.builder()
                .value(value)
                .locale(locale)
                .build();
        }
    }


    @XmlAttribute(name = "lang", namespace = XMLConstants.XML_NS_URI)
    @JsonProperty("lang")
    @XmlJavaTypeAdapter(value = XmlLangAdapter.class)
    @Getter
    @Setter
    private Locale locale;

    @XmlValue
    @NonNull
    @Getter
    @Setter
    private String value;

    @Getter
    @Setter
    private String charsetName;


    public LocalizedString() {

    }

    @lombok.Builder
    private LocalizedString(Locale locale, @NonNull String value, String charsetName) {
        this.locale = locale;
        this.value = value;
        this.charsetName = charsetName;
    }

    public static String get(Locale locale, Iterable<LocalizedString> strings) {
        LocalizedString candidate = null;
        if (strings != null) {
            int score = -1;
            for (LocalizedString string : strings) {
                int s = score(string.getLocale(), locale);
                if (s > score) {
                    candidate = string;
                    score = s;
                }
            }
        }
        return candidate == null ? null : candidate.getValue();

    }

    @Override
    public int length() {
        return value.length();

    }

    @Override
    public char charAt(int index) {
        return value.charAt(index);

    }

    @Override
    public LocalizedString subSequence(int start, int end) {
        return LocalizedString.of(value.substring(start, end), locale);

    }

    @NonNull
    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LocalizedString that = (LocalizedString) o;

        if (!locale.equals(that.locale)) return false;
        if (!value.equals(that.value)) return false;
        return charsetName != null ? charsetName.equals(that.charsetName) : that.charsetName == null;
    }

    @Override
    public int hashCode() {
        int result = locale.hashCode();
        result = 31 * result + value.hashCode();
        result = 31 * result + (charsetName != null ? charsetName.hashCode() : 0);
        return result;
    }

    public static class XmlLangAdapter extends XmlAdapter<String, Locale> {

        @Override
        public Locale unmarshal(String v) {
            return adapt(v);

        }

        @Override
        public String marshal(Locale v) {
            return v == null ? null : v.toString();

        }
    }


    public static Locale adapt(String v) {
        if (v == null) {
            return null;
        }
        String[] split = v.split("[_-]", 3);
        LanguageCode languageCode = LanguageCode.getByCode(split[0], false);
        String language = languageCode == null ? split[0] : languageCode.name().toLowerCase();

        switch (split.length) {
            case 1:
                return new Locale(language);
            case 2:
                return new Locale(language, split[1].toUpperCase());
            default:
                return new Locale(language, split[1].toUpperCase(), split[2]);
        }
    }
}
