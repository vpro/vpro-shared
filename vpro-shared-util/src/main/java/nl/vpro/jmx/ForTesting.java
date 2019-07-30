package nl.vpro.jmx;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.management.DescriptorKey;
import javax.management.MXBean;

import static java.lang.annotation.ElementType.*;

/**
 * This annotation can be used to  mark an mx bean (annotation with {@link MXBean} operation as for testing only.
 * See also {@link Description}
 * @author Michiel Meeuwissen
 * @since 1.79
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({METHOD, PARAMETER, TYPE})
public @interface ForTesting {
    @DescriptorKey("reason")
    String value();
}
