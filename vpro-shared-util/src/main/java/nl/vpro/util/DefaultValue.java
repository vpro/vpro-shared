package nl.vpro.util;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Recognized by {@link nl.vpro.guice.OptionalModule}
 * @author Michiel Meeuwissen
 * @since 1.69
 */
@Retention(RUNTIME)
public @interface DefaultValue {
    String value();
}
