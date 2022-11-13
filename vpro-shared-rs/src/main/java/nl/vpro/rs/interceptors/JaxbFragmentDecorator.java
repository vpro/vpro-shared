package nl.vpro.rs.interceptors;

import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;

import org.jboss.resteasy.spi.DecoratorProcessor;

import nl.vpro.rs.XmlFragment;

@Slf4j
public class JaxbFragmentDecorator implements DecoratorProcessor<Marshaller, XmlFragment>
 {
    @Override
    public Marshaller decorate(Marshaller target, XmlFragment annotation,
                               Class type, Annotation[] annotations, MediaType mediaType)
    {
        try {
            target.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        } catch (PropertyException e) {
            log.warn(e.getMessage(), e);
        }
        return target;
    }

}
