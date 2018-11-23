package nl.vpro.elasticsearchclient;

import lombok.extern.slf4j.Slf4j;

import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

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
            new HttpHost("localhost", 9209, "http"))
            .build();
    }


    @Test
    public void test() {
        ElasticSearchIterator<String> i = ElasticSearchIterator
            .<String>builder()
            .client(client)
            .adapt(jsonNode -> jsonNode.get(Constants.Fields.ID).textValue())
            .build();
        ObjectNode search = i.prepareSearch("pageupdates-publish");
        QueryBuilder.asc(search, "lastPublished");
        ObjectNode query = search.with(Constants.QUERY);
        QueryBuilder.mustTerm(query, "broadcasters", "VPRO");

        i.forEachRemaining((u) -> {
            if (i.getCount() % 1000 == 0) {
                log.info("{}/{}: {} (eta: {})", i.getCount(), i.getTotalSize().orElse(null), u,
                    i.getETA().map(eta -> eta.atZone(ZoneId.of("Europe/Amsterdam")).toLocalDateTime().truncatedTo(ChronoUnit.SECONDS)).orElse(null)
                );
            }
        });

     }


    @Test
    public void testSources() {
        ElasticSearchIterator<JsonNode> i = ElasticSearchIterator.sources(client);
        i.setJsonRequests(false);
        JsonNode search = i.prepareSearch("pageupdates-publish");
        i.forEachRemaining((node) -> {
            String url = node.get("url").textValue();
            if (i.getCount() % 1000 == 0) {
                log.info("{}: {}", i.getCount(), url);

            }
        });
    }

     @Test
    public void test15() {

        try (ElasticSearchIterator<JsonNode> i = ElasticSearchIterator
             .sources(client);) {
            i.setJsonRequests(false);
            ObjectNode search = i.prepareSearch("apimedia", "program", "group", "segment");

            i.forEachRemaining((node) -> {
                String string;
                if (node.has("mid")) {
                    string = node.get("mid").textValue();
                } else {
                    string = node.toString();
                }
                if (i.getCount() % 1000 == 0) {
                    log.info("{}: {}", i.getCount(), string);

                }
            });
        }
    }

    @Test
    public void testmemberref() {

        ElasticSearchIterator<JsonNode> i = ElasticSearchIterator
            .sources(client);
        i.setJsonRequests(false);

        ObjectNode search = i.prepareSearch("apimedia-publish", "groupMemberRef", "episodeRef");
        ObjectNode query = search.with("query");
        QueryBuilder.mustTerm(query, "midRef", "18Jnl1100");


        i.forEachRemaining((node) -> {
            log.info("{}: {}", i.getCount(), node);
        });
    }

}
