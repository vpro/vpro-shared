package nl.vpro.elasticsearchclient;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 1.75
 */
@Slf4j
public class IndexHelperITest {


    RestClient client;
    IndexHelper helper;
    @BeforeEach
    public void setup() {

        client = RestClient.builder(
            new HttpHost("localhost", 9200, "http"))
            .build();

        helper = IndexHelper.builder()
            .log(log)
            .client(new SimpleESClientFactory(client, () -> "simple"))
            .settingsResource("/setting.json")
            .mappingResource("/test.json")
            .indexName("test-" + System.currentTimeMillis())
            .build();

        helper.createIndexIfNotExists();
    }



    @AfterEach
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
    public void writeJson() throws ExecutionException, InterruptedException {
        helper.setWriteJsonDir(new File("/tmp"));
        TestObject test = new TestObject();
        test.setId("a");
        test.setTitle("bla");

        final List<BulkRequestEntry> jobs = Arrays.asList(helper.indexRequest(test.getId(), test));
        helper.bulkAsync(jobs, new Consumer<ObjectNode>() {
            @Override
            public void accept(ObjectNode jsonNodes) {
                ArrayNode items = jsonNodes.withArray("items");
                log.info("{}", jobs);
                for (JsonNode i : items) {
                    log.info("{}", IndexHelper.find(jobs, (ObjectNode) i));

                }

            }
        }).get();
    }


    @Test
    public void unalias() {
        log.info("{}", helper.unalias("apipagequeries"));
    }

}
