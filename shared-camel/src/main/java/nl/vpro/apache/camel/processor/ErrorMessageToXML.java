/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.apache.camel.processor;

import java.io.StringWriter;
import java.io.Writer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * @author Roelof Jan Koekoek
 * @since 1.10
 */
public class ErrorMessageToXML<T> implements Processor {
    private Class<T> clazz;

    public ErrorMessageToXML(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        Throwable caused = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class);
        if(caused == null) {
            return;
        }

        StringBuilder sb = new StringBuilder("<!-- ")
            .append(caused.getMessage())
            .append(" -->\n");

        T body = exchange.getIn().getBody(clazz);
        JAXBContext context = JAXBContext.newInstance(clazz);
        Marshaller m = context.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        m.setProperty("com.sun.xml.internal.bind.xmlHeaders", sb.toString());
        Writer writer = new StringWriter();
        m.marshal(body, writer);
        exchange.getIn().setBody(writer.toString());
    }
}
