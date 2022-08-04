package nl.vpro.rs;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.xml.bind.Marshaller;

import org.jboss.resteasy.annotations.Decorator;

import nl.vpro.rs.interceptors.JaxbFragmentDecorator;

/**
 * Annotating a resteasy method with this, will cause the produced xml to be a 'fragment', i.e. it will lack
 * the {@code <?xml } prefix.
 * @since 2.33.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Decorator(processor = JaxbFragmentDecorator.class, target = Marshaller.class)
public @interface XmlFragment {
}
