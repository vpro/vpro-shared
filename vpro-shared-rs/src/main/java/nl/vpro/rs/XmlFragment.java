package nl.vpro.rs;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotating a resteasy method with this, will cause the produced xml to be a 'fragment', i.e. it will lack
 * the {@code <?xml } prefix.
 * @since 2.33.0
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface XmlFragment {
}
