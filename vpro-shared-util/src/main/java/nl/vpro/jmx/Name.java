package nl.vpro.jmx;

import java.lang.annotation.*;

import javax.management.DescriptorKey;

import static java.lang.annotation.ElementType.*;

/**
 * This annotation can be used to add a name to a mx bean parameter
 *
 * @author Michiel Meeuwissen
 * @since 2.18
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({PARAMETER})
public @interface Name {
    @DescriptorKey("name")
    String value();

}
