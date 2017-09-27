package nl.vpro.couchdb;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertTrue;


/**
 * Stolen from https://code.google.com/p/jcouchdb/source/browse/trunk/test/org/jcouchdb/db/OptionsTestCase.java?r=232
 * @author Michiel Meeuwissen
 * @since 1.0
 */
public class CouchdbOptionsTest {



    @Test
    public void option() {
        String query = new CouchdbOptions("foo", 1).put("bar", "baz!").toQuery();

        assertTrue(query.startsWith("?"));
        assertTrue(query.contains("foo=1"));
        assertTrue(query.contains("bar=baz%21"));


        query = new CouchdbOptions("foo", 1).put("bar", new ArrayList()).toQuery();
        assertTrue(query.startsWith("?"));
        assertTrue(query.contains("foo=1"));
        assertTrue(query.contains("bar=%5B%5D"));

        query = new CouchdbOptions().startKeyDocId("bar").toQuery();
        assertTrue(query.startsWith("?"));
        assertTrue(query.contains("startkey_docid=bar"));
    }

    @Test
    public void testDynamicAccess() {
        List<String> keys = Arrays.asList(
                "key",
                "startkey",
                "endkey",
                "endkey_docid",
                "limit",
                "update",
                "descending",
                "skip",
                "group",
                "stale",
                "reduce",
                "include_docs");

        for (String key : keys) {
            String value = (String) new CouchdbOptions().put(key, "abc").get(key);
            boolean shouldBeEncoded = CouchdbOptions.JSON_ENCODED_OPTIONS.contains(key);
            assertTrue(value.equals(shouldBeEncoded ? "\"abc\"" : "abc"));
        }
    }
}

