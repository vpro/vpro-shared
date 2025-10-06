package nl.vpro.i18n;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serial;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;

import javax.xml.XMLConstants;
import jakarta.xml.bind.annotation.*;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.PolyNull;
import org.meeuw.i18n.languages.*;

import com.fasterxml.jackson.annotation.JsonProperty;

import static nl.vpro.i18n.Locales.score;

/**
 * Basically wraps a string together with the {@link Locale} describing in what language it is.
 *
 * @author Michiel Meeuwissen
 * @since 3.2
 */
@Getter
@XmlAccessorType(XmlAccessType.NONE)
@XmlTransient
@Slf4j
public class LocalizedString implements CharSequence, Serializable {

    @Serial
    private static final long serialVersionUID = 6128871258866551736L; //implements javax.xml.registry.infomodel.LocalizedString {

    @PolyNull
    public static LocalizedString of(@PolyNull String value, Locale locale) {
        if (value == null) {
            return null;
        } else {
            return LocalizedString.builderOf(value)
                .locale(locale)
                .build();
        }
    }

    public static LocalizedString.Builder builderOf(String value) {
        return builder().value(value);
    }

    @XmlAttribute(name = "lang", namespace = XMLConstants.XML_NS_URI)
    @JsonProperty("lang")
    @XmlJavaTypeAdapter(value = XmlLangAdapter.class)
    @Setter
    @Schema(implementation = String.class, type = "string")
    private Locale locale;

    @XmlValue
    @NonNull
    @Setter
    private String value;

    @Setter
    private String charsetName;

    public LocalizedString() {

    }

    @lombok.Builder(toBuilder = true)
    protected LocalizedString(
        Locale locale,
        @NonNull String value,
        String charsetName) {
        this.locale = locale;
        this.value = value;
        this.charsetName = charsetName;
    }

    protected LocalizedString(
        LocalizedString copy) {
        this.locale = copy.locale;
        this.value = copy.value;
        this.charsetName = copy.charsetName;
    }

    public static String get(Locale locale, Iterable<? extends LocalizedString> strings) {
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
        return Objects.equals(charsetName, that.charsetName);
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
        String language;
        try {
            ISO_639_Code languageCode = ISO_639.lenientIso639(split[0]);
            language = languageCode == null ? split[0] : languageCode.code().toLowerCase();
        } catch (LanguageNotFoundException languageNotFoundException) {
            language = split[0];
        }

        return switch (split.length) {
            case 1 -> new Locale(language);
            case 2 -> new Locale(language, split[1].toUpperCase());
            default -> new Locale(language, split[1].toUpperCase(), split[2]);
        };
    }
    public static class Builder implements Supplier<CharSequence> {
        public Builder charset(Charset charset) {
            return charsetName(charset.name());
        }

        @Override
        public CharSequence get() {
            return build();
        }
    }

    @XmlRootElement(name = "localizedString")
    public static class Impl extends LocalizedString {

        @Serial
        private static final long serialVersionUID = -6556571754244929512L;

        public Impl() {

        }
        public Impl(LocalizedString copy) {
            super(copy);
        }
    }

    public static Impl impl(String value, Locale locale) {
        return new Impl(of(value, locale));
    }
}
