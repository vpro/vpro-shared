package nl.vpro.apache.camel.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * https://jira.vpro.nl/browse/MSE-1703
 * @author Michiel Meeuwissen
 * @since 1.8
 */
public class RemoveControlCharacters implements Processor {

    @Override
    public void process(Exchange exchange) {
        String body = exchange.getIn().getBody(String.class);
        String replaced = body.replaceAll("\\p{Cc}", "");
        exchange.getIn().setBody(replaced);
    }
}
