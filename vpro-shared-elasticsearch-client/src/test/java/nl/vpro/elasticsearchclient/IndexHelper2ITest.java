package nl.vpro.elasticsearchclient;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

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
            new HttpHost("localhost", 9204, "http"))
            .build();

        helper = IndexHelper.builder()
            .log(log)
            .client((e) -> client)
            .indexName("apimedia")
            .build();
    }



    @Test
    public void mget() {
        log.info("{}", helper.get(Arrays.asList("program", "group"), "RBX_KRO_2439590"));
    }


    @Test
    public void mget404() {
        log.info("{}", helper.get(Arrays.asList("program", "group"), "BESTAATNIET"));
    }

}
