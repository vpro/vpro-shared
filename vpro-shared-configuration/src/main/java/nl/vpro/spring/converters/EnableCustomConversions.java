package nl.vpro.spring.converters;

import java.lang.annotation.*;

import org.springframework.context.annotation.Import;


/**
 * Enabled a bunch of custom versions by VPRO, mainly related to converting to Durations and so on.
 *
 * @since 5.0
 * @see CustomConversion
 * @see nl.vpro.spring.converters
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({CustomConversion.class})
public @interface EnableCustomConversions {
}
