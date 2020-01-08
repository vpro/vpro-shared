package nl.vpro.test.jupiter;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * In combination with {@link AbortOnException}. A test annotated with this will not be skipped, even if a previous test failed.
 *
 * Sometimes in integrations test some later test mainly perform clean up actions, and you'd better not skip it.
 *
 * @author Michiel Meeuwissen
 * @since 2.10
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface NoAbort {
}
