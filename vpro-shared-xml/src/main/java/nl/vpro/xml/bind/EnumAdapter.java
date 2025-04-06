package nl.vpro.xml.bind;

import lombok.SneakyThrows;

import java.lang.reflect.Field;
import java.util.*;

import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * This is a nice idea. The sad thing however, is that when using this, the generated XSD will not anymore be an enum.
 *
 * @author Michiel Meeuwissen
 * @since 1.63
 */
public abstract class EnumAdapter<T extends Enum<T>> extends XmlAdapter<String,  T> {

    private final Map<String, T> map;
    private final boolean ignoreCase;

    protected EnumAdapter(Class<T> enumClass) {
        this.map = map(enumClass, true);
        this.ignoreCase = true;
    }

    @SneakyThrows
    protected static <T extends Enum<T>> Map<String, T> map(Class<T> enumClass, boolean ignoreCase) {
        Map<String, T> result = new HashMap<>();
        for (Field f : enumClass.getDeclaredFields()) {
            if (f.isEnumConstant()) {
                XmlEnumValue value = f.getAnnotation(XmlEnumValue.class);
                T constant = (T) f.get(null);
                String key;
                if (value != null) {
                    key = ignoreCase ? value.value().toUpperCase() : value.value();
                } else {
                    key = ignoreCase ? f.getName().toUpperCase() : f.getName();
                }
                result.put(key, constant);
            }
        }
        for (Field f : enumClass.getDeclaredFields()) {
            if (f.isEnumConstant()) {
                XmlEnumValue value = f.getAnnotation(XmlEnumValue.class);
                T constant = (T) f.get(null);
                String key;
                if (value != null) {
                    key = ignoreCase ? f.getName().toUpperCase() : f.getName();
                    if (!result.containsKey(key)) {
                        result.put(key, constant);
                    }
                }
            }
        }
        return Collections.unmodifiableMap(result);
    }

    protected T valueOf(@NonNull String v) {
        if (v == null) {
            throw new NullPointerException("v must not be null");
        }
        if (ignoreCase) {
            v = v.toUpperCase();
        }
        v = v.trim();
        T result = map.get(v);
        if (result == null) {
            throw new IllegalArgumentException("No enum constant " + v);

        }
        return result;
    }

    @Override
    public T unmarshal(String v) throws Exception {
        if (v == null) {
            return null;
        }
        try {
            return valueOf(v.trim());
        } catch (IllegalArgumentException iae) {
            try {
                return valueOf(v.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new JAXBException(e);
            }
        }

    }

    @Override
    public String marshal(T v) {
        if (v == null) {
            return null;
        }
        return v.name();

    }
}
