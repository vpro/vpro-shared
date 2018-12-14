package nl.vpro.elasticsearch;

import org.junit.Test;

import nl.vpro.util.UrlProvider;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Michiel Meeuwissen
 * @since ...
 */
public class TransportClientFactoryTest {

    @Test
    public void setElasticSearchHosts() {
        TransportClientFactory factory = new TransportClientFactory();
        factory.setElasticSearchHosts("hosta,hostb:9301");

        assertThat(factory.getTransportAddresses()).containsExactly(new UrlProvider("hosta", 9301), new UrlProvider("hostb", 9301));
    }

}
