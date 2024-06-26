package nl.vpro.configuration.spring.converters;

import java.lang.annotation.*;

import org.springframework.context.annotation.Import;


/**
 * Enables a bunch of custom versions by VPRO, mainly related to converting to Durations and so on.
 * <p>
 * E.g. when this is effective, this will work:
 * <pre>
 *     {@literal @}Value("${nep.itemizer.ratelimiter.unit:1H}") Duration unit, // defaults to 1 hour
 * </pre>
 * In this example {@link StringToDurationConverter} is triggered.
 * <p>
 * Typically, you would put this annotation on a class with is also annotated with {@link org.springframework.context.annotation.Configuration}. E.g. like this:
 * <pre>
 * {@literal @}Configuration
 * {@literal @}EnableCustomConversions
 * {@literal @}Import({OpenApiConfig.class, AOPConfig.class, JPAConfig.class, SecurityConfiguration.class})
 * {@literal @}ImportResource(locations = "classpath*:META-INF/vpro/*-context.xml")
 *  public class AppConfig  {
 * ..
 * </pre>
 *
 * @since 5.0
 * @see CustomConversion
 * @see nl.vpro.configuration.spring.converters all converters in this package are available then
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({CustomConversion.class})
public @interface EnableCustomConversions {
}
