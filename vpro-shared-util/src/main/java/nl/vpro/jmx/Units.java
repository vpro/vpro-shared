package nl.vpro.jmx;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.management.DescriptorKey;
import javax.management.MXBean;

import static java.lang.annotation.ElementType.*;

/**
 * This annotation can be used to add a unit to some numeric value in a mx bean (annotation with {@link MXBean}.
 * <p>
 * E.g.
 *<pre>
 *{@code
 *
 * @MXBean
 * public interface CounterMXBean {
 *     @Description("Current event rate averaged over a period")
 *     @Units("events/minute")
 *     double getRate();
 *     ...
 * }
 *}
 *</pre>
 * See also {@link Description}
 * @author Michiel Meeuwissen
 * @since 1.57
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({METHOD, PARAMETER, TYPE})
public @interface Units {
    @DescriptorKey("units")
    String value();
}
