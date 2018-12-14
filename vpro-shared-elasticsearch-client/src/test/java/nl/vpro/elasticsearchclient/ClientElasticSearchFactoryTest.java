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
        factory.setUnicastHosts("hosta,hostb:9200");



        assertThat(factory.getHosts()).containsExactly(new HttpHost("hosta", 9200), new HttpHost("hostb", 9200));


    }

}
