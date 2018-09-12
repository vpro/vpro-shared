package nl.vpro.elasticsearchclient;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.Arrays;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 1.75
 */
@Slf4j
public class IndexHelperITest {


    RestClient client;
    IndexHelper helper;
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
            .indexName("test-" + System.currentTimeMillis())
            .build();

        helper.createIndexIfNotExists();
    }



    @After
    public void shutdown() {
        helper.deleteIndex();
        helper.refresh();
        assertThat(helper.checkIndex()).isFalse();
    }

    @Test
    public void checkExists() {
        assertThat(helper.checkIndex()).isTrue();
    }

    @Test
    public void count() {
        assertThat(helper.count()).isEqualTo(0);
    }


    @Test
    public void getClusterName() {
        log.info("clustername: {}", helper.getClusterName());
    }

    @Test
    public void writeJson() {
        helper.setWriteJsonDir(new File("/tmp"));
        TestObject test = new TestObject();
        test.setId("a");
        test.setTitle("bla");

        helper.bulk(Arrays.asList(helper.indexRequest("test",  test.getId(), test)));
    }


}
