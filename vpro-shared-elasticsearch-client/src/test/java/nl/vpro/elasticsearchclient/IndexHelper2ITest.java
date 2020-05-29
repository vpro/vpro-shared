package nl.vpro.elasticsearchclient;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/**
 * @author Michiel Meeuwissen
 * @since 1.75
 */
@Slf4j
public class IndexHelper2ITest {


    RestClient client;
    IndexHelper helper;
    @BeforeEach
    public void setup() {

        client = RestClient.builder(
            new HttpHost("localhost", 9221, "http"))
            .build();

        helper = IndexHelper.builder()
            .log(log)
            .client(new SimpleESClientFactory(client))
            .indexName("apimedia_refs")
            .build();
    }



    @Test
    public void mget() {
        log.info("{}", helper.get(Arrays.asList("program", "group"), "RBX_KRO_2439590"));
    }


    @Test
    public void mget2() {
        log.info("{}", helper.mgetWithRouting(new IndexHelper.RoutedId("AUTO_WEKKERWAKKER/391/RBX_MAX_2248446/episodeRef", "AUTO_WEKKERWAKKER")));
    }


    @Test
    public void mget404() {
        log.info("{}", helper.get(Arrays.asList("program", "group"), "BESTAATNIET"));
    }

}
