package nl.vpro.elasticsearchclient;

import lombok.extern.slf4j.Slf4j;

import java.time.ZoneId;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
        ElasticSearchIterator<String> i = ElasticSearchIterator
            .<String>builder()
            .client(client)
            .adapt(jsonNode -> jsonNode.get(Constants.ID).textValue())
            .build();
        ObjectNode search = i.prepareSearch("pageupdates-publish");
        QueryBuilder.asc(search, "lastPublished");
        ObjectNode query = search.with(Constants.QUERY);
        QueryBuilder.mustTerm(query, "broadcasters", "VPRO");

        i.forEachRemaining((u) -> {
            log.info("{}/{}: {} (eta: {})", i.getCount(), i.getTotalSize().orElse(null), u,
                i.getETA().map(eta -> eta.atZone(ZoneId.of("Europe/Amsterdam")).toLocalDateTime()).orElse(null)
            );
        });

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
