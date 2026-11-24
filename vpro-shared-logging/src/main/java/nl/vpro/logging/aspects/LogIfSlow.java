package nl.vpro.logging.aspects;

import java.lang.annotation.*;

import org.springframework.context.annotation.EnableAspectJAutoProxy;


/**
 * Can be added to methods. If LogIfSlowAspect is in effect (as a spring bean, and {@link EnableAspectJAutoProxy} is on) , then it will cause a warning to be issued if the method took longer
 * then {@link #warnThreshold()} or an error if even longer then {@link #errorThreshold()}.
 *
 * @author Michiel Meeuwissen
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface LogIfSlow {


    /**
     * Issue a (log4j2) warn if the method took longer that this. Specified as a string that will be parsed by {@link java.time.Duration#parse(CharSequence)}
     * <p>
     * Defaults to {@link #errorThreshold()}{@code /2}
     * <p>
     * Will not happen if an error (because of {@link #errorThreshold()} was issued already.
     */
    String warnThreshold() default "";

    /**
     * Issue a (log4j2) error if the method took longer that this. Specified as a string that will be parsed by {@link java.time.Duration#parse(CharSequence)}
     */
    String errorThreshold() default "PT20s";

    /**
     * Specify the log4j2 name to use for logging. If not specified, it will be based on the target class of the annotation.
     */
    String logger() default "";
}
