package nl.vpro.rs.interceptors;

import jakarta.ws.rs.core.MediaType;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.PropertyException;
import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;

import org.jboss.resteasy.annotations.DecorateTypes;
import org.jboss.resteasy.spi.DecoratorProcessor;

import nl.vpro.rs.XmlFragment;

@Slf4j
@DecorateTypes({"text/*+xml", "application/*+xml"})
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
