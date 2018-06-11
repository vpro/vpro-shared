package nl.vpro.jmx;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.management.DescriptorKey;
import javax.management.MXBean;

import static java.lang.annotation.ElementType.*;

/**
 * This annotation can be used to add a description to a mx bean (annotation with {@link MXBean}, or its methods.
 *
 * See also {@link Units} for a way to describe the unit of a JMX property.
 *
 * @author Michiel Meeuwissen
 * @since 1.57
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({CONSTRUCTOR, METHOD, PARAMETER, TYPE})
public @interface Description {
    @DescriptorKey("description")
    String value();

}
