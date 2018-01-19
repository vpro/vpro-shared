package nl.vpro.elasticsearchclient;

import lombok.extern.slf4j.Slf4j;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
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
    }




    @Test
    public void checkExists() {
        assertThat(helper.checkIndex()).isFalse();
    }

    @Test
    public void createIndex() {
        helper.prepareIndex();
        helper.refresh();
        assertThat(helper.count()).isEqualTo(0);
        helper.deleteIndex();
        helper.refresh();
        assertThat(helper.checkIndex()).isFalse();
    }

    @Test
    public void getClusterNamee() {
        log.info("clustername: {}", helper.getClusterName());
    }


}
