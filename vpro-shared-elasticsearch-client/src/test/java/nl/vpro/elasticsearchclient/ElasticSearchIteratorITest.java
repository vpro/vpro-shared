package nl.vpro.elasticsearchclient;

import lombok.extern.slf4j.Slf4j;

import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import nl.vpro.util.Version;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 0.47
 */
@Ignore("Requires actualy es connection")
@Slf4j
public class ElasticSearchIteratorITest {

    static int ID  = 0;
    public static class A {
        public String id = String.valueOf(ID++);
        public String title = "bar";
    }


    RestClient client;
    IndexHelper helper;

    String index = "test-" + System.currentTimeMillis();
    @Before
    public void setup() {

        client = RestClient.builder(
            new HttpHost("localhost", 9200, "http"))
            .build();

        helper = IndexHelper.builder()
            .log(log)
            .client((e) -> client)
            .settingsResource("setting.json")
            .mappingResource("test.json")
            .indexName(index)
            .build();
        log.info("Version: {}", helper.getVersionNumber());

        helper.createIndexIfNotExists();
        for (int i = 0; i < 200; i++) {
            A a = new A();
            helper.index("test", a.id, a);
        }
        helper.refresh();

    }
    @After
    public void shutdown() {
        helper.deleteIndex();
    }


    @Test
    public void test() {
        try (ElasticSearchIterator<String> i = ElasticSearchIterator
            .<String>builder()
            .client(client)
            .adapt(jsonNode -> jsonNode.get(Constants.Fields.ID).textValue())
            .build()) {
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

     }


    @Test
    public void testSources() {
        try (ElasticSearchIterator<JsonNode> i = ElasticSearchIterator.sources(client);) {
            i.setJsonRequests(false);
            JsonNode search = i.prepareSearch("pageupdates-publish");
            i.forEachRemaining((node) -> {
                String url = node.get("url").textValue();
                if (i.getCount() % 1000 == 0) {
                    log.info("{}: {}", i.getCount(), url);

                }
            });
        }
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
    public void testAll() {
        try (ElasticSearchIterator<JsonNode> i = ElasticSearchIterator
            .sourcesBuilder(client)
            .autoEsVersion()
            .build()
            ;) {
            i.setJsonRequests(false);

            ObjectNode search = i.prepareSearch(index, "test");
            //ObjectNode query = search.with("query");

            AtomicLong count = new AtomicLong(0);
            i.forEachRemaining((node) -> {
                log.info("{}/{}: {}", i.getCount(), i.getTotalSize().get(), node);
                count.incrementAndGet();
            });
            assertThat(count.get()).isEqualTo(200);
        }
    }

     @Test
    public void testmemberref() {

        try (ElasticSearchIterator<JsonNode> i = ElasticSearchIterator
            .sourcesBuilder(client)
            .esVersion(Version.of(5))
            .build()) {
            i.setJsonRequests(false);

            ObjectNode search = i.prepareSearch("apimedia-publish", "groupMemberRef", "episodeRef");
            ObjectNode query = search.with("query");
            QueryBuilder.mustTerm(query, "childRef", "18Jnl1100");


            i.forEachRemaining((node) -> {
                log.info("{}/{}: {}", i.getCount(), i.getTotalSize().get(), node);

            });
        }
    }

}
