package nl.vpro.elasticsearchclient;

import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

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
            new HttpHost("localhost", 9208, "http"))
            .build();
    }


    @Test
    public void test() {
        ElasticSearchIterator<Map<String, Object>> i = new ElasticSearchIterator<>(client, (jn) -> (Map<String, Object>) Jackson2Mapper.getLenientInstance().convertValue(jn, Map.class));
        ObjectNode node = i.prepareSearch(Collections.singleton("media"), null);
        node.put("size", 100);


        while(i.hasNext()) {
            String mid = String.valueOf(i.next().get("mid"));
            System.out.println("" + i.getCount() + "/" + i.getTotalSize().orElse(null) + " " + mid);
        }
     }


    @Test
    // NOT used as this doesn't work on ES 1....
    public void correctPageIds() {
        ElasticSearchIterator<JsonNode> i = ElasticSearchIterator.of(client);
        i.prepareSearch("apipages");
        long index = 0;
        while(i.hasNext()) {
            JsonNode node = i.next();
            String id = node.get("_id").textValue();
            String url = node.get("_source").get("url").textValue();
            if (! Objects.equals(id, url)) {
                log.info("{}, {}", id, url);
            }
            if (index++ % 1000 == 0) {
                log.info("{}: {}", index, url);

            }
        }
    }
}
