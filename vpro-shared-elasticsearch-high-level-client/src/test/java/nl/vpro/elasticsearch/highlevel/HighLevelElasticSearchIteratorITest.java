package nl.vpro.elasticsearch.highlevel;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.NoSuchElementException;

import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.jupiter.api.*;

import com.fasterxml.jackson.databind.JsonNode;

import nl.vpro.elasticsearch.CreateIndex;
import nl.vpro.elasticsearch.ElasticSearchIteratorInterface;
import nl.vpro.elasticsearchclient.ClientElasticSearchFactory;
import nl.vpro.elasticsearchclient.IndexHelper;
import nl.vpro.logging.simple.StringBuilderSimpleLogger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Michiel Meeuwissen
 * @since 2.22
 */
@Slf4j
class HighLevelElasticSearchIteratorITest {


    static HighLevelClientFactory highLevelClientFactory;
    static IndexHelper helper;
    static {
        ClientElasticSearchFactory clientElasticSearchFactory = new ClientElasticSearchFactory();
        clientElasticSearchFactory.setHosts("localhost:9200");
        clientElasticSearchFactory.setClusterName(System.getProperty("integ.cluster.name", "elasticsearch"));
        highLevelClientFactory = new HighLevelClientFactory(clientElasticSearchFactory);

        helper = IndexHelper.builder()
            .client(highLevelClientFactory)
            .settingsResource("/settings.json")
            .mappingResource("/test.json")
            .indexName("test-" + System.currentTimeMillis())
            .simpleLogger(new StringBuilderSimpleLogger())
            .build();
        helper.createIndex(CreateIndex.FOR_TEST);
        String[] titles = {"foo", "bar"};
        for (int i = 0; i < 100; i++) {
            A a = new A(titles[i % titles.length], i);
            helper.indexWithRouting(a.id, a, "a");
        }
        helper.refresh();
    }


    static int ID  = 0;

    @AfterAll
    public static void cleanup() {
        helper.deleteIndex();
    }
    @AfterEach
    public void check() {
        assertThat(ElasticSearchIteratorInterface.getScrollIds()).isEmpty();
    }


    @Test
    public void iterateA() {
        long count = 0;
        try (HighLevelElasticSearchIterator<A> iterator = HighLevelElasticSearchIterator.<A>builder()
            .client(highLevelClientFactory.highLevelClient())
            .adaptTo(A.class)
            .routing("a")
            .build()) {
            SearchSourceBuilder sourceBuilder = iterator.prepareSearchSource(helper.getIndexName());

            TermQueryBuilder query = new TermQueryBuilder("title", "foo");
            sourceBuilder.size(40);
            sourceBuilder.query(query);

            assertThat(iterator.getTotalSize()).contains(50L);
            assertThat(iterator.getSizeQualifier()).contains(ElasticSearchIteratorInterface.TotalRelation.EQUAL_TO);

            assertThat(iterator.getResponse().getScrollId()).isNotNull();
            log.info("Iterating: {}", iterator);
            while(iterator.hasNext()) {
                A next = iterator.next();
                assertThat(next.title).isEqualTo("foo");
                if (iterator.getCount() % 5 == 0) {
                    log.info("{}: {} {}", next, iterator.getSpeed(), iterator.getFraction());
                }
                assertThat(count++).isEqualTo(iterator.getCount());
            }
            assertThatThrownBy(iterator::next).isInstanceOf(NoSuchElementException.class);
        }
        assertThat(count).isEqualTo(50);
    }

    @Test
    public void iterateSearchHit() {
        long count = 0;
        try (HighLevelElasticSearchIterator<SearchHit> iterator = HighLevelElasticSearchIterator.searchHits(highLevelClientFactory.highLevelClient())) {
            SearchSourceBuilder sourceBuilder = iterator.prepareSearchSource(helper.getIndexName());

            TermQueryBuilder query = new TermQueryBuilder("title", "bar");
            sourceBuilder.size(13);
            sourceBuilder.query(query);

            iterator.start();

            assertThatThrownBy(iterator::start).isInstanceOf(IllegalStateException.class);

            assertThat(iterator.getSizeQualifier()).contains(ElasticSearchIteratorInterface.TotalRelation.EQUAL_TO);

            assertThat(iterator.getResponse().getScrollId()).isNotNull();
            log.info("Iterating: {}", iterator);
            while(iterator.hasNext()) {
                SearchHit next = iterator.next();
                if (iterator.getCount() % 5 == 0) {
                    log.info("{}: {} {}", next, iterator.getRate(), iterator.getFraction());
                }
                assertThat(count++).isEqualTo(iterator.getCount());
            }
            assertThatThrownBy(iterator::next).isInstanceOf(NoSuchElementException.class);
        }
        assertThat(count).isEqualTo(50);
    }

     @Test
    public void iterateSources() {
        long count = 0;
        try (HighLevelElasticSearchIterator<JsonNode> iterator = HighLevelElasticSearchIterator.sources(highLevelClientFactory.highLevelClient())) {

            SearchSourceBuilder sourceBuilder = iterator.prepareSearchSource(helper.getIndexName());
            TermQueryBuilder query = new TermQueryBuilder("title", "bar");
            sourceBuilder.query(query);

            assertThat(iterator.getResponse().getScrollId()).isNotNull();
            log.info("Iterating: {}", iterator);
            while(iterator.hasNext()) {
                JsonNode next = iterator.next();
                assertThat(next.get("title").textValue()).isEqualTo("bar");
                if (iterator.getCount() % 5 == 0) {
                    log.info("{}: {} {}", next, iterator.getRate(), iterator.getFraction());
                }
                assertThat(count++).isEqualTo(iterator.getCount());
            }
            assertThatThrownBy(iterator::next).isInstanceOf(NoSuchElementException.class);
        }
        assertThat(count).isEqualTo(50);
    }

    @Test
    public void iterateString() {
        long count = 0;
        try (HighLevelElasticSearchIterator<String> iterator = HighLevelElasticSearchIterator.<String>builder()
            .client(highLevelClientFactory.highLevelClient())
            .adapt(SearchHit::getId)
            .beanName("hltest")
            .scrollContext(Duration.ofMillis(100))
            .requestOptions(RequestOptions.DEFAULT.toBuilder().addHeader("X-a", "a").build())
            .build()) {

            assertThatThrownBy(iterator::getTotalSize).isInstanceOf(IllegalStateException.class);

            iterator.prepareSearchSource(helper.getIndexName()).query(
                 new WildcardQueryBuilder("title", "*")
            );


            assertThat(iterator.getResponse().getScrollId()).isNotNull();
            log.info("Iterating: {}", iterator);
            while(iterator.hasNext()) {
                String next = iterator.next();
                assertThat(next).matches("\\d+");
                count++;
            }
        }
        assertThat(count).isEqualTo(100);
    }

    @Test
    public void findNothing() {
        long count = 0;
        try (HighLevelElasticSearchIterator<String> iterator = HighLevelElasticSearchIterator.<String>builder()
            .client(highLevelClientFactory.highLevelClient())
            .adapt(SearchHit::getId)
            .build()) {

            iterator.prepareSearchSource(helper.getIndexName()).query(
                 new TermQueryBuilder("title", "xxyy")
            );

            assertThat(iterator.getResponse().getScrollId()).isNotNull();
            log.info("Iterating: {}", iterator);
            while(iterator.hasNext()) {
                iterator.next();
                count++;
            }
        }
        assertThat(count).isEqualTo(0);
    }

    @Test
    public void illegalConstructions()  {
        assertThatThrownBy(() -> HighLevelElasticSearchIterator.<String>builder()
            .client(highLevelClientFactory.highLevelClient())
            .scrollContext(Duration.ofMillis(-100)).build()).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> HighLevelElasticSearchIterator.<String>builder()

            .build()).isInstanceOf(NullPointerException.class).hasMessage("client is marked non-null but is null");

    }

}
