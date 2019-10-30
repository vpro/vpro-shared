package nl.vpro.i18n;

import lombok.extern.slf4j.Slf4j;

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
public class LocalizedString implements CharSequence { //implements javax.xml.registry.infomodel.LocalizedString {



    public static LocalizedString of(String value, Locale locale) {
        if (value == null) {
            return null;
        } else {
            LocalizedString string = new LocalizedString();
            string.value = value;
            string.locale = locale;
            return string;
        }
    }



    @XmlAttribute(name = "lang", namespace = XMLConstants.XML_NS_URI)
    @JsonProperty("lang")
    @XmlJavaTypeAdapter(value = XmlLangAdapter.class)
    private Locale locale;

    @XmlValue
    @NonNull
    private String value;

    private String charset;

    //@Override
    public String getCharsetName()  {
        return charset;

    }

    //@Override
    public Locale getLocale() {
        return locale;
    }

    //@Override
    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    //@Override
    public String getValue() {
        return value;
    }

    //@Override
    public void setCharsetName(String charsetName) {
        this.charset = charsetName;

    }

    //@Override
    public void setValue(String value) {
        this.value = value;
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
