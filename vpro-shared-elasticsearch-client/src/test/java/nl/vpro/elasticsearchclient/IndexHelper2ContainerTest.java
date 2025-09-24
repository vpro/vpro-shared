package nl.vpro.elasticsearchclient;

import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.*;
import org.testcontainers.junit.jupiter.Testcontainers;

import nl.vpro.test.opensearch.ElasticsearchContainer;


/**
 * @author Michiel Meeuwissen
 * @since 1.75
 */
@Slf4j
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IndexHelper2ContainerTest {
    ElasticsearchContainer es = new ElasticsearchContainer(true);


    RestClient client;
    IndexHelper helper;
    @BeforeEach
    public void setup() {

        client = RestClient.builder(es.getHttpHost())
            .build();

        helper = IndexHelper.builder()
            .log(log)
            .client(new SimpleESClientFactory(client))
            .indexName("apimedia_refs")
            .build();
    }



    @Test
    public void mget2() {
        log.info("{}", helper.mgetWithRouting(
                new IndexHelper.RoutedId("AUTO_WEKKERWAKKER/1104/RBX_MAX_13070966/episodeRef", "AUTO_WEKKERWAKKER"),
                new IndexHelper.RoutedId("AUTO_WEKKERWAKKER/1096/RBX_MAX_13070914/episodeRe", "AUTO_WEKKERWAKKER")
        ));

    }


    @Test
    public void mget404() {
        log.info("{}", helper.get("BESTAATNIET"));
    }

}
