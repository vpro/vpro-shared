package nl.vpro.elasticsearch;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import org.elasticsearch.client.Client;
import org.elasticsearch.search.SearchHit;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import nl.vpro.util.UrlProvider;

/**
 * @author Michiel Meeuwissen
 * @since 0.47
 */
@Ignore("Requires actualy es connection")
@Slf4j
public class ElasticSearchIteratorTest {

    Client client;
    @Before
    public void setup() {
        ESClientFactoryImpl factory = new ESClientFactoryImpl();
        factory.setTransportAddresses(Collections.singletonList(new UrlProvider("localhost", 9308)));
        factory.setSniffCluster(false);
        factory.setIgnoreClusterName(true);

        client = factory.client("test");
    }


    @Test
    public void test() {
        ElasticSearchIterator<Map<String, Object>> i = new ElasticSearchIterator<>(client, SearchHit::getSource);
        i.prepareSearch("apimedia");
        while(i.hasNext()) {
            String mid = String.valueOf(i.next().get("mid"));
            System.out.println("" + i.getCount() + "/" + i.getTotalSize().orElse(null) + " " + mid);
        }
     }


    @Test
    public void correctPageIds() throws Exception {
        ElasticSearchIterator<SearchHit> i = new ElasticSearchIterator<>(client, s -> s);
        String indexName = "apipages";
        i.prepareSearch(indexName);
        long index = 0;
        while(i.hasNext()) {
            SearchHit node = i.next();
            String id = node.getId();
            Map<String, Object> source = node.getSource();
            String type = node.getType();
            String url = node.getSource().get("url").toString();

            if (! Objects.equals(id, url)) {
                String sourceAsString = node.getSourceAsString();
                reindex(indexName, id, type, url, sourceAsString);
                log.info("{}, {}", id, url);
            }
            if (index++ % 1000 == 0) {
                log.info("{}: {}", index, url);

            }
        }
    }

    protected void reindex(String indexName, String oldId, String type, String id, String object) throws Exception {

        client.prepareIndex(indexName, type, id).setSource(object.getBytes(Charset.forName("UTF-8"))).execute().get();
        client.prepareDelete().setIndex(indexName).setId(oldId).setType(type).execute().get();
    }
}
