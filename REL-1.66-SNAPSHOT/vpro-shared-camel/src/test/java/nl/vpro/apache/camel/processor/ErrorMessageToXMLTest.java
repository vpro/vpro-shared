/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.apache.camel.processor;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Roelof Jan Koekoek
 * @since 1.10
 */
public class ErrorMessageToXMLTest extends CamelTestSupport {

    @Produce(uri = "direct:in")
    protected ProducerTemplate in;

    @Produce(uri = "direct:error")
    protected ProducerTemplate error;

    @Test
    public void testNormalOperation() throws Exception {
        getMockEndpoint("mock:out").expectedMessageCount(1);

        in.sendBody(new SomeJAXBMessage());

        assertMockEndpointsSatisfied();
    }

    @Test
    @Ignore("How to get mock output...")
    public void testDeadletter() throws Exception {
        getMockEndpoint("mock:deadletter").expectedBodiesReceived(
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<!-- Error message -->\n" +
                "<root>\n" +
                "    <child>Text value</child>\n" +
                "</root>"
        );

        error.sendBody(new SomeJAXBMessage());

        assertMockEndpointsSatisfied();
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            public void configure() {

                onException(Exception.class)
                    .process(new ErrorMessageToXML<SomeJAXBMessage>(SomeJAXBMessage.class))
                    .to("mock:deadletter");

                from("direct:in")
                    .process(new ErrorMessageToXML<SomeJAXBMessage>(SomeJAXBMessage.class))
                    .to("mock:out");

                from("direct:error")
                    .process(new Processor() {
                        @Override
                        public void process(Exchange exchange) throws Exception {
                            throw new RuntimeException("Error message");
                        }
                    })
                    .to("mock:out");
            }
        };
    }

    @XmlRootElement(name = "root")
    public static class SomeJAXBMessage {

        @XmlElement(name = "child")
        private String element = "Text value";
    }
}
