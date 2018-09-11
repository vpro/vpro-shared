package nl.vpro.elasticsearchclient;

import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Map;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import nl.vpro.jackson2.Jackson2Mapper;

/**
 * @author Michiel Meeuwissen
 * @since 0.47
 */
@Ignore("Requires actualy es connection")
@Slf4j
public class ElasticSearchIteratorITest {

    RestClient client;
    @Before
    public void setup() {

        client = RestClient.builder(
            new HttpHost("localhost", 9212, "http"))
            .build();
    }


    @Test
    public void test() {
        ElasticSearchIterator<Map<String, Object>> i = ElasticSearchIterator
            .<Map<String, Object>>builder()
            .client(client)
            .adapt(jsonNode -> (Map<String, Object>) Jackson2Mapper.getLenientInstance().convertValue(jsonNode, Map.class))
            .build();
        ObjectNode node = i.prepareSearch(Collections.singleton("media"), null);
        node.put("size", 100);


        while(i.hasNext()) {
            String mid = String.valueOf(i.next().get("mid"));
            System.out.println("" + i.getCount() + "/" + i.getTotalSize().orElse(null) + " " + mid);
        }
     }


    @Test
    public void testSources() {
        ElasticSearchIterator<JsonNode> i = ElasticSearchIterator.sources(client);
        JsonNode search = i.prepareSearch("pageupdates-publish");
        i.forEachRemaining((node) -> {
            String url = node.get("url").textValue();
            if (i.getCount() % 1000 == 0) {
                log.info("{}: {}", i.getCount(), url);

            }
        });
    }
}
