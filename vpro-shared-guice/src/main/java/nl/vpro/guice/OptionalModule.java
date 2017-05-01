package nl.vpro.guice;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.time.Duration;
import java.util.Optional;

import javax.inject.Named;

import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.multibindings.OptionalBinder;
import com.google.inject.name.Names;

import nl.vpro.util.DefaultValue;
import nl.vpro.util.TimeUtils;

/**
 * @author Michiel Meeuwissen
 * @since 1.69
 */
@Slf4j
public class OptionalModule extends AbstractModule {

    private final Class[] classes;

    public OptionalModule(Class... classes) {
        this.classes = classes;
    }

    @Override
    protected void configure() {
        configure(binder(), classes);
    }

    @SuppressWarnings("unchecked")
    public static void configure(Binder binder, Class... classes) {
        for (Class clazz : classes) {
            for (Field f : clazz.getDeclaredFields()) {
                if (Optional.class.isAssignableFrom(f.getType())) {
                    try {
                        Class valueClass = Class.forName(((ParameterizedType) f.getGenericType()).getActualTypeArguments()[0].getTypeName());
                        Named annotation = f.getAnnotation(Named.class);
                        if (annotation != null) {

                            OptionalBinder optionalBinder = OptionalBinder.newOptionalBinder(binder,
                                Key.get(valueClass, Names.named(annotation.value())));
                            DefaultValue defaultValue = f.getAnnotation(DefaultValue.class);
                            if (defaultValue != null) {
                                String value = defaultValue.value();
                                // TODO don't we have access to guice type convertors?
                                if (valueClass.isInstance(value)) {
                                    optionalBinder.setDefault().toInstance(value);
                                } else if (Duration.class.isAssignableFrom(valueClass)) {
                                    optionalBinder.setDefault().toInstance(TimeUtils.parseDuration(value).orElse(null));

                                }
                            }
                        }
                    } catch (ClassNotFoundException e) {
                        log.error(e.getMessage(), e);
                    }
                }
            }
        }
    }
}
