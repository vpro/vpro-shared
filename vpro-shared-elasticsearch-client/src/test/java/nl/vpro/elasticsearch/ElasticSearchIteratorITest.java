package nl.vpro.elasticsearch;

import java.util.Map;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import nl.vpro.jackson2.Jackson2Mapper;

/**
 * @author Michiel Meeuwissen
 * @since 0.47
 */
@Ignore("Requires actualy es connection")
public class ElasticSearchIteratorITest {

    RestClient client;
    @Before
    public void setup() {

        client = RestClient.builder(
            new HttpHost("localhost", 9200, "http"))
            .build();
    }


    @Test
    public void test() {
        ElasticSearchIterator<Map<String, Object>> i = new ElasticSearchIterator<>(client, (jn) -> (Map<String, Object>) Jackson2Mapper.getLenientInstance().convertValue(jn, Map.class));
        ObjectNode node = i.prepareSearch("media");
        node.put("size", 100);


        while(i.hasNext()) {
            String mid = String.valueOf(i.next().get("mid"));
            System.out.println("" + i.getCount() + "/" + i.getTotalSize().orElse(null) + " " + mid);
        }
     }
}
