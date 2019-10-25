package nl.vpro.elasticsearch;

import java.util.Map;

import org.elasticsearch.client.Client;
import org.elasticsearch.search.SearchHit;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import nl.vpro.util.UrlProvider;

/**
 * @author Michiel Meeuwissen
 * @since 0.47
 */
@Ignore("Requires actualy es connection")
public class ElasticSearchIteratorITest {

    Client client;
    @Before
    public void setup() {
        TransportClientFactory factory = new TransportClientFactory();
        factory.setTransportAddresses(new UrlProvider("localhost", 9300));
        //factory.setSniffCluster(false);
        factory.setIgnoreClusterName(true);

        client = factory.client("test");
    }


    @Test
    public void test() {
        ElasticSearchIterator<Map<String, Object>> i = new ElasticSearchIterator<>(client, SearchHit::getSourceAsMap);
        i.prepareSearch("apimedia");
        while(i.hasNext()) {
            String mid = String.valueOf(i.next().get("mid"));
            System.out.println("" + i.getCount() + "/" + i.getTotalSize().orElse(null) + " " + mid);
        }
     }
}
