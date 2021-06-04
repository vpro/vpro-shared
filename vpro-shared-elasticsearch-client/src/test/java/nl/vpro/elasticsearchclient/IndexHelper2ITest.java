package nl.vpro.elasticsearchclient;

import lombok.extern.slf4j.Slf4j;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.*;


/**
 * @author Michiel Meeuwissen
 * @since 1.75
 */
@Slf4j
@Disabled
public class IndexHelper2ITest {


    RestClient client;
    IndexHelper helper;
    @BeforeEach
    public void setup() {

        client = RestClient.builder(
            new HttpHost("localhost", 9200, "http"))
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
