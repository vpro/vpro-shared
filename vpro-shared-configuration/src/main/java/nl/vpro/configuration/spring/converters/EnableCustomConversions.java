package nl.vpro.configuration.spring.converters;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;


/**
 * Enabled a bunch of custom versions by VPRO, mainly related to converting to Durations and so on.
 * @since 5.0
 * @see CustomConversion
 * @see nl.vpro.spring.converters
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({CustomConversion.class})
public @interface EnableCustomConversions {
}
