package nl.vpro.elasticsearch.highlevel;

import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import com.google.common.base.MoreObjects;

import nl.vpro.elasticsearch.CreateIndex;
import nl.vpro.elasticsearchclient.ClientElasticSearchFactory;
import nl.vpro.elasticsearchclient.IndexHelper;
import nl.vpro.logging.simple.StringBuilderSimpleLogger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since ...
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

    public static class A {
        public String id = String.valueOf(ID++);
        public String title = "bar";
        public int value;

        public A() {

        }
        public A(String title, int value) {
            this.title = title;
            this.value = value;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("title", title)
                .add("value", value)
                .toString();
        }
    }

    @AfterAll
    public static void cleanup() {
        helper.deleteIndex();
    }


    @Test
    public void iterator() {
        int count = 0;
        try (HighLevelElasticSearchIterator<A> iterator = HighLevelElasticSearchIterator.<A>highLevelBuilder()
            .client(highLevelClientFactory.highLevelClient(HighLevelElasticSearchIterator.class.getName()))
            .adaptTo(A.class)
            .routing("a")
            .build()) {
            SearchSourceBuilder sourceBuilder = iterator.prepareSearchSource(helper.getIndexName());

            TermQueryBuilder query = new TermQueryBuilder("title", "foo");
            sourceBuilder.query(query);


            while(iterator.hasNext()) {
                A next = iterator.next();
                assertThat(next.title).isEqualTo("foo");
                log.info("{}", next);
                count++;
            }
        }
        assertThat(count).isEqualTo(50);
    }

}
