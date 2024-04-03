package nl.vpro.configuration.spring.converters;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;


/**

 * @since 5.0
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({CustomConversion.class})
public @interface EnableCustomConversions {
}
