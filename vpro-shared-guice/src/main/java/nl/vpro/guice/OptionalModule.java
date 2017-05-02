package nl.vpro.guice;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.*;
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
 @ Uses {@link OptionalBinder} to provide <code>null</code>'s or <code>@DefaultValue</code>'s for <code>@Named</code> parameters of the type <code>Optional</code>
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


    public static void configure(Binder binder, Class... classes) {
        for (Class clazz : classes) {
            for (Field f : clazz.getDeclaredFields()) {
                bind(binder, f, f.getType(), f.getGenericType());
            }
            for (Constructor c : clazz.getConstructors()) {
                for (Parameter parameter : c.getParameters())  {
                    bind(binder, parameter, parameter.getType(), parameter.getParameterizedType());
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected static void bind(Binder binder, AnnotatedElement f, Class<?> type, Type parameterizedType) {
        if (Optional.class.isAssignableFrom(type)) {
            try {
                Named annotation = f.getAnnotation(Named.class);
                if (annotation != null) {
                    Class valueClass = Class.forName(((ParameterizedType) parameterizedType).getActualTypeArguments()[0].getTypeName());
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
