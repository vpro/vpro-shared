package nl.vpro.couchdb;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Used to pass query CouchdbOptions to view queries.
 * For example:
 * <pre>
 * database.queryView("company/all", Map.class,
 *     new CouchdbOptions().count(1).descending(true));
 * </pre>
 * <p/>
 * <p/>
 * In contrast to earlier versions of this class it became clear that some CouchdbOptions
 * needs to be JSON encoded and some CouchdbOptions musn't be JSON encoded. There is no way around that,
 * that's just the way CouchDB works.
 * <p/>
 * Internally, this class keeps a list of CouchdbOptions that need JSON encoding:
 * <ul>
 * <li>key</li>
 * <li>startkey</li>
 * <li>endkey</li>
 * </ul>
 * <p/>
 * It will automatically encode those option names. If you need to have non-supported CouchdbOptions encoded
 * you have to subclass CouchdbOptions and then access {@link #putEncoded(String, Object)}.
 *
 * @author Michiel Meeuwissen (copied from  org.jcouchdb.db.Options with reduced dependencies)
 */
public class CouchdbOptions {
    private static final long serialVersionUID = -4025495141211906568L;

    private static ObjectMapper mapper = new ObjectMapper();

    private Map<String, Object> content = new LinkedHashMap<>();

    final static Set<String> JSON_ENCODED_OPTIONS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "key",
        "startkey",
        "endkey"
    )));

    public CouchdbOptions() {

    }

    public CouchdbOptions(Map<String, Object> map) {
        for(Map.Entry<String, Object> e : map.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    /**
     * Copies the CouchdbOptions of the given CouchdbOptions object if it is not <code>null</code>.
     *
     * @param CouchdbOptions CouchdbOptions to be copied, can be <code>null</code>.
     */
    public CouchdbOptions(CouchdbOptions CouchdbOptions) {
        if(CouchdbOptions != null) {
            // CouchdbOptions values are allready encoded thus need all to be added unencoded
            for(String key : CouchdbOptions.keys()) {
                putUnencoded(key, CouchdbOptions.get(key));
            }
        }
    }

    public CouchdbOptions(String key, Object value) {
        putUnencoded(key, value);
    }

    public CouchdbOptions put(String key, Object value) {
        if(JSON_ENCODED_OPTIONS.contains(key)) {
            return putEncoded(key, value);
        } else {
            return putUnencoded(key, value);
        }
    }

    protected CouchdbOptions putEncoded(String key, Object value) {
        try {
            String json = mapper.writeValueAsString(value);
            content.put(key, json);
            return this;
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected CouchdbOptions putUnencoded(String key, Object value) {
        content.put(key, value);
        return this;
    }

    public CouchdbOptions key(Object key) {
        return putEncoded("key", key);
    }

    public CouchdbOptions startKey(Object key) {
        return putEncoded("startkey", key);
    }

    public CouchdbOptions startKeyDocId(String docId) {
        return putUnencoded("startkey_docid", docId);
    }

    public CouchdbOptions endKey(Object key) {
        return putEncoded("endkey", key);
    }

    public CouchdbOptions endKeyDocId(String docId) {
        return putUnencoded("endkey_docid", docId);
    }

    public CouchdbOptions limit(int limit) {
        return putUnencoded("limit", limit);
    }

    public CouchdbOptions update(boolean update) {
        return putUnencoded("update", update);
    }

    public CouchdbOptions descending(boolean update) {
        return putUnencoded("descending", update);
    }

    public CouchdbOptions since(long scince) {
        return putUnencoded("since", scince);
    }

    public CouchdbOptions skip(long skip) {
        return putUnencoded("skip", skip);
    }

    public CouchdbOptions group(boolean group) {
        return putUnencoded("group", group);
    }

    public CouchdbOptions stale() {
        return putUnencoded("stale", "ok");
    }

    public CouchdbOptions reduce(boolean reduce) {
        return putUnencoded("reduce", reduce);
    }

    public CouchdbOptions includeDocs(boolean includeDocs) {
        return putUnencoded("include_docs", includeDocs);
    }

    public CouchdbOptions groupLevel(int level) {
        return putUnencoded("group_level", level);
    }

    public String toQuery() {
        StringBuilder sb = new StringBuilder();
        sb.append("?");

        boolean first = true;
        try {
            for(String key : keys()) {
                if(!first) {
                    sb.append("&");
                }
                sb.append(key).append("=");
                sb.append(URLEncoder.encode(get(key).toString(), "UTF-8"));
                first = false;
            }
            if(sb.length() <= 1) {
                return "";
            } else {
                return sb.toString();
            }
        } catch(UnsupportedEncodingException e) {
            throw new RuntimeException("error converting option value", e);
        }
    }

    public Object get(String key) {
        return content.get(key);
    }

    /**
     * Can be imported statically to have a syntax a la <code>option().count(1);</code>.
     *
     * @return new Option instance
     */
    public static CouchdbOptions option() {
        return new CouchdbOptions();
    }

    public Set<String> keys() {
        return content.keySet();
    }

}
