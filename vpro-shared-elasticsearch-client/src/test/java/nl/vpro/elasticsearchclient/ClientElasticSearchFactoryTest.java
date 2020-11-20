package nl.vpro.elasticsearchclient;

import org.apache.http.HttpHost;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
public class ClientElasticSearchFactoryTest {


    @Test
    public void setUnicastHosts() {

        ClientElasticSearchFactory factory = new ClientElasticSearchFactory();
        factory.setHosts("hosta,hostb:9200");
        assertThat(factory.getHttpHosts()).containsExactly(new HttpHost("hosta", 9200), new HttpHost("hostb", 9200));


    }

    @Test
    public void setUnicastHosts443() {

        ClientElasticSearchFactory factory = new ClientElasticSearchFactory();
        factory.setHosts("https://a.es.amazonaws.com,https://b.es.amazonaws.com:4043");
        assertThat(factory.getHttpHosts()).containsExactly(new HttpHost("a.es.amazonaws.com", -1, "https"), new HttpHost("b.es.amazonaws.com", 4043, "https"));


    }

}
