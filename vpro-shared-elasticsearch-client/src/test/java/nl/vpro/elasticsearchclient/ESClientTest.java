package nl.vpro.elasticsearchclient;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
@Slf4j
public class ESClientTest {


    @Test
    @Ignore("Connects to real server")
    public void test() {

        RestClient client = RestClient.builder(
            new HttpHost("localhost", 9212, "http"))
            .build();

        try (ElasticSearchIterator<String> i = ElasticSearchIterator
            .<String>builder()
            .client(client)
            .adapt(jsonNode -> jsonNode.get(Constants.Fields.ID).textValue())
            .build()) {
            ObjectNode search = i.prepareSearch("apimedia", "program", "segment", "group");
            QueryBuilder.asc(search, "publishDate");
            ObjectNode query = search.with(Constants.QUERY);
            QueryBuilder.mustTerm(query, "workflow", "REVOKED");
            AtomicInteger count = new AtomicInteger(0);
            i.forEachRemaining((u) -> {
                //System.out.println(count.incrementAndGet() + " " + u);
                System.out.println(u);
            });
        }

    }
}
