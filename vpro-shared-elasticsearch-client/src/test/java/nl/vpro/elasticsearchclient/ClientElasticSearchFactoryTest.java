package nl.vpro.elasticsearchclient;

import org.apache.http.HttpHost;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
public class ClientElasticSearchFactoryTest {


    @Test
    public void setHosts() {
        ClientElasticSearchFactory factory = new ClientElasticSearchFactory();
        factory.setHosts("hosta,hostb:9200");
        assertThat(factory.getHttpHosts()).containsExactly(
            new HttpHost("hosta", 9200),
            new HttpHost("hostb", 9200)
        );
    }

    @Test
    public void setHosts9201() {
        ClientElasticSearchFactory factory = new ClientElasticSearchFactory();
        factory.setHosts("(hosta,hostb):9201");
        assertThat(factory.getHttpHosts()).containsExactly(
            new HttpHost("hosta", 9201),
            new HttpHost("hostb", 9201)
        );
        assertThat(factory.getHosts()).isEqualTo("hosta:9201,hostb:9201");
    }

    @Test
    public void setHosts443() {

        ClientElasticSearchFactory factory = new ClientElasticSearchFactory();
        factory.setHosts("https://a.es.amazonaws.com,https://b.es.amazonaws.com:4043");
        assertThat(factory.getHttpHosts()).containsExactly(
            new HttpHost("a.es.amazonaws.com", -1, "https"),
            new HttpHost("b.es.amazonaws.com", 4043, "https")
        );

    }

    @Test
    public void setHostsDefault() {
        ClientElasticSearchFactory factory = new ClientElasticSearchFactory();
        factory.setHosts("");
        assertThat(factory.getHttpHosts()).containsExactly(
            new HttpHost("localhost", 9200)
        );
    }

}
