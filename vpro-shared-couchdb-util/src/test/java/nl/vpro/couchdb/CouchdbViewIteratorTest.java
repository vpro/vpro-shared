package nl.vpro.couchdb;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import static org.junit.Assert.assertEquals;

/**
 * @author Michiel Meeuwissen
 * @since 1.0
 */
public class CouchdbViewIteratorTest {

    @Test
    public void test() throws IOException {
        InputStream inputStream = CouchdbViewIterator.class.getResourceAsStream("/exampleview.json");
        CouchdbViewIterator iterator = new CouchdbViewIterator(inputStream);
        List<JsonNode> results = new ArrayList<JsonNode>();
        while(iterator.hasNext()) {
            JsonNode node = iterator.next();
            results.add(node);
        }
        assertEquals(2, results.size());
        assertEquals("urn:vpro:media:program:14367824", results.get(0).get("doc").get("urn").asText());
        assertEquals("urn:vpro:media:program:14389752", results.get(1).get("doc").get("urn").asText());
    }

    @Test
    public void testAllDocs() throws IOException {
        InputStream inputStream = CouchdbViewIterator.class.getResourceAsStream("/alldocs.json");
        CouchdbViewIterator iterator = new CouchdbViewIterator(inputStream);
        List<JsonNode> results = new ArrayList<JsonNode>();
        while(iterator.hasNext()) {
            JsonNode node = iterator.next();
            results.add(node);
        }
        assertEquals(5, results.size());
        assertEquals("urn:vpro:media:group:20814338", results.get(0).get("doc").get("urn").asText());
        assertEquals("urn:vpro:media:group:20809885", results.get(1).get("doc").get("urn").asText());
        assertEquals("urn:vpro:media:group:20719987", results.get(2).get("doc").get("urn").asText());
        assertEquals("urn:vpro:media:group:20720014", results.get(3).get("doc").get("urn").asText());
        assertEquals("urn:vpro:media:group:20709174", results.get(4).get("doc").get("urn").asText());
    }

}
