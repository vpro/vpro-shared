package nl.vpro.elasticsearch;

import java.time.Duration;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * @author Michiel Meeuwissen
 */
public class Constants {

    @NonNull
    public static Duration ES_LATENCY = Duration.ofSeconds(10);

    public static class Fields {
        public static final String TYPE    = "_type";
        public static final String ID      = "_id";
        public static final String INDEX   = "_index";
        public static final String ROUTING = "_routing";
        public static final String PARENT  = "_parent";
        public static final String SOURCE  = "_source";
        public static final String VERSION  = "_version";
        public static final String SCORE = "_score";
        public static final String ERROR = "error";


        public static final String DOC  = "doc";
        public static final String DOC_AS_UPSERT  = "doc_as_upsert";
    }


    public static final String HITS = "hits";
    public static final String _SCROLL_ID = "_scroll_id";
    public static final String SCROLL_ID = "scroll_id";
    public static final String SCROLL = "scroll";

    public static final String QUERY = "query";
    public static final String P_QUERY = "/" + QUERY;
    public static final String SCRIPT= "script";
    public static final String VERSION = "version";
    public static final String SORT = "sort";
    public static final String ORDER = "order";
    public static final String DESC = "desc";
    public static final String ASC = "asc";
    public static class Query {
        public static final String BOOL = "bool";
        public static final String P_BOOL = "/" + BOOL;
        public static final String MUST = "must";
        public static final String P_MUST = "/" + MUST;
        public static final String SHOULD = "should";
        public static final String P_SHOULD = "/" + SHOULD;
        public static final String RANGE = "range";
        public static final String P_RANGE = "/" + RANGE;
        public static final String TERM = "term";
        public static final String P_TERM = "/" + TERM;
        public static final String WILDCARD = "wildcard";
        public static final String P_WILDCARD = "/" + WILDCARD;
        public static final String TERMS = "terms";
        public static final String P_TERMS = "/" + TERMS;
        public static final String PREFIX = "prefix";
        public static final String P_PREFIX = "/" + PREFIX;
        public static final String SIZE = "size";

    }

    public static final String FILTER = "filter";


    /**
     * Deprecated if not {@link #DOC}
     */
    public static final String TYPE     = "type";
    public static final String ID       = "id";
    public static final String PARENT   = "parent";
    public static final String ROUTING  = "routing";
    public static final String INDEX    = "index";
    public static final String P_INDEX = "/" + INDEX;
    public static final String UPDATE   = "update";
    public static final String P_UPDATE = "/" + UPDATE;
    public static final String DELETE = "delete";
    public static final String P_DELETE = "/" + DELETE;
    public static final String SETTINGS = "settings";
    public static final String P_SETTINGS = "/" + SETTINGS;


    public static final String RETRY_ON_CONFLICT = "retry_on_conflict";
    public static final String DOC       = "_doc";

    public static class Paths {
        public static final String SCROLL = "/_search/scroll";
        public static final String SEARCH = "/_search";
        public static final String DELETE_BY_QUERY = "/_delete_by_query";
        public static final String UPDATE_BY_QUERY = "/_update_by_query";
        public static final String COUNT = "/_count";
        public static final String SETTINGS = "/_settings";
        public static final String MAPPING= "/_mapping";
        public static final String BULK = "/_bulk";


    }

    public static class Methods {
        public static final String POST = "POST";
        public static final String GET = "GET";
        public static final String PUT = "PUT";
        public static final String METHOD_DELETE = "DELETE";
        public static final String HEAD = "HEAD";
    }

    public static class Mappings {
        public static final String PROPERTIES = "properties";

    }

}
