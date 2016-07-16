package nl.vpro.elasticsearch;

import java.util.Collections;
import java.util.Map;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHit;
import org.junit.Before;
import org.junit.Test;

import nl.vpro.util.UrlProvider;

/**
 * @author Michiel Meeuwissen
 * @since 0.47
 */
public class ElasticSearchIteratorTest {

    Client client;
    @Before
    public void setup() {
        ESClientFactoryImpl factory = new ESClientFactoryImpl();
        factory.setTransportAddresses(Collections.singletonList(new UrlProvider("localhost", 9302)));
        factory.setSniffCluster(false);
        factory.setIgnoreClusterName(true);

        client = factory.client("test");
    }

    
    @Test
    public void test() {
        ElasticSearchIterator<Map<String, Object>> i = new ElasticSearchIterator<>(client, SearchHit::getSource);
        i.prepareSearch("apimedia");
        while(i.hasNext()) {
            String mid = String.valueOf(i.next().get("mid"));
            System.out.println("" + i.getCount() + "/" + i.getTotalSize().orElse(null) + " " + mid);
        }
     }
}