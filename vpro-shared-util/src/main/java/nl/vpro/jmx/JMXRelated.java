package nl.vpro.jmx;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.*;

/**
 * You may add this to fields and method to mark that they are related to JMX. E.g. sometimes there is no more reason to have a field then to expose it via managed attribute.
 * <p>
 * This annotation just indicates then that that's all.
 * @since 5.4
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({METHOD, FIELD})
public @interface JMXRelated {
}
