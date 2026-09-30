package nl.vpro.elasticsearchclient;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.node.ObjectNode;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import nl.vpro.elasticsearch.Constants;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
@Slf4j
public class ESClientTest {


    @Test
    @Disabled("Connects to real server")
    public void test() {

        RestClient client = RestClient.builder(
            new HttpHost("localhost", 9212, "http"))
            .build();

        try (ElasticSearchIterator<String> i = ElasticSearchIterator
            .<String>builder()
            .client(client)
            .adapt(jsonNode -> jsonNode.get(Constants.Fields.ID).stringValue())
            .build()) {
            ObjectNode search = i.prepareSearchOnIndices("apimedia");
            QueryBuilder.asc(search, "publishDate");
            ObjectNode query = search.withObject(Constants.QUERY);
            QueryBuilder.mustTerm(query, "workflow", "REVOKED");
            AtomicInteger count = new AtomicInteger(0);
            //System.out.println(count.incrementAndGet() + " " + u);
            i.forEachRemaining(System.out::println);
        }

    }
}
