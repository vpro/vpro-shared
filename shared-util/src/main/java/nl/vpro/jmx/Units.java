package nl.vpro.jmx;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.management.DescriptorKey;

import static java.lang.annotation.ElementType.*;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({METHOD, PARAMETER, TYPE})
public @interface Units {
    @DescriptorKey("units")
    String value();
}
