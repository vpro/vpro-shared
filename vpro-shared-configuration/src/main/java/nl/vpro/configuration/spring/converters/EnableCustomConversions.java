package nl.vpro.configuration.spring.converters;

import java.lang.annotation.*;

import org.springframework.context.annotation.Import;


/**
 * Enables a bunch of custom versions by VPRO, mainly related to converting to Durations and so on.
 * <p>
 * E.g. when this is effective, this will work:
 * <pre>
 *     {@literal @}Value("${nep.itemizer.ratelimiter.unit:PT1H}") Duration unit,
 * </pre>
 * This triggers {@link StringToDurationConverter}.
 *
 * @since 5.0
 * @see CustomConversion
 * @see nl.vpro.configuration.spring.converters all converters in this pacakge are available then
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({CustomConversion.class})
public @interface EnableCustomConversions {
}
