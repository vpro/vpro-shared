package nl.vpro.jackson2;

import java.lang.annotation.*;

/**
 * @author Michiel Meeuwissen
 * @since 2.17
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AfterUnmarshal {
}
