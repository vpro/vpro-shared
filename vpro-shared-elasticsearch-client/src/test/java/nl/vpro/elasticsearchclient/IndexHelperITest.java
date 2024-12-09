package nl.vpro.elasticsearchclient;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.*;
import org.slf4j.event.Level;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import nl.vpro.jackson2.Jackson2Mapper;
import nl.vpro.logging.simple.*;
import nl.vpro.test.opensearch.ElasticsearchContainer;

import static nl.vpro.elasticsearch.Constants.Fields.SOURCE;
import static nl.vpro.logging.simple.Slf4jSimpleLogger.slf4j;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Michiel Meeuwissen
 * @since 1.75
 */
@Slf4j
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IndexHelperITest {

    ElasticsearchContainer es = new ElasticsearchContainer(true);


    RestClient client;
    IndexHelper helper;

    final Queue<Event> events = new ArrayDeque<>();
    final SimpleLogger simpleLogger = QueueSimpleLogger.of(events).chain(slf4j(log));

    @BeforeEach
    public void setup() {
        events.clear();
        client = RestClient.builder(es.getHttpHost())
            .build();

        helper = IndexHelper.builder()
            .simpleLogger(simpleLogger)
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
    public void getClusterName() throws ExecutionException, InterruptedException {
        log.info("clustername: {}", helper.getClusterName());
        log.info("distribution: {}", helper.getInfo().get().getDistribution());
        log.info("info: {}", helper.getInfo().get());
    }

    @Test
    public void getActualSettings() throws IOException {
        log.info("settings: {}", helper.getActualSettings());
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

    @Test
    public void indexWrong() {
        assertThatThrownBy(() -> {
            TestObject test = new TestObject();
            test.setId("id");
            test.setTitle("wrong");
            ObjectNode jsonNode = Jackson2Mapper.getPublisherInstance().valueToTree(test);
            jsonNode.put("unrecognizedField", "foobar");
            helper.index(test.getId(), jsonNode);
        }).isInstanceOf(RuntimeException.class);
        List<Event> warnings  = events.stream().filter(e -> e.getLevelInt() > Level.INFO.toInt()).collect(Collectors.toList());
        assertThat(warnings).hasSize(1);
        log.info("{}", warnings);
    }


    @Test
    public void bulkWrong() {
        TestObject test = new TestObject();
        test.setId("id");
        test.setTitle("wrong");
        ObjectNode jsonNode = Jackson2Mapper.getPublisherInstance().valueToTree(test);
        jsonNode.put("unrecognizedField", "foobar");
        BulkRequestEntry bulkRequestEntry = helper.indexRequest(test.getId(), jsonNode);
        ObjectNode result = helper.bulk(Arrays.asList(bulkRequestEntry));
        helper.bulkLogger().accept(result);
        log.info("{}", result);

        List<Event> warnings  = events.stream().filter(e -> e.getLevelInt() > Level.INFO.toInt()).collect(Collectors.toList());
        assertThat(warnings).hasSize(1);
        log.info("{}", warnings);
    }

    @Test
    public void indexAndGetAndUpdateAndDelete() {
        helper.refresh();
        long before = helper.count();
        TestObject test = new TestObject();
        test.setId("https://www-acc.vpro.nl/speel~WO_VPRO_442926~interview-frédérik-ruys~.html");
        test.setTitle("ok");

        helper.index(test.getId(), test);

        helper.refresh();
        log.info("CReading indexing");
        Optional<JsonNode> jsonNode1 = helper.get(test.getId());
        assertThat(jsonNode1).isPresent();
        log.info("{}", jsonNode1);

        helper.refresh();
        test.setTitle("ok2");;
        helper.index(test.getId(), test);
        helper.refresh();
        log.info("CReading indexing");
        Optional<JsonNode> got2 = helper.get(test.getId());
        assertThat(got2).isPresent();
        log.info("{}", got2);
        assertThat(got2.get().get(SOURCE).get("title").asText()).isEqualTo("ok2");



        helper.delete(test.getId());
        helper.refresh();
        assertThat(helper.count()).isEqualTo(before);
    }


}
