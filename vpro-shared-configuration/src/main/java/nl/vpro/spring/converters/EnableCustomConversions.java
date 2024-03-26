package nl.vpro.spring.converters;

import java.lang.annotation.*;

import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;

import com.google.common.annotations.Beta;


/**

 * @since 5.0
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({CustomConversion.class})
@Beta
@Order(1)
public @interface EnableCustomConversions {
}
