package nl.vpro.elasticsearchclient;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Michiel Meeuwissen
 * @since 1.75
 */
@Slf4j
@Ignore
public class IndexHelper2ITest {


    RestClient client;
    IndexHelper helper;
    @Before
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

}
