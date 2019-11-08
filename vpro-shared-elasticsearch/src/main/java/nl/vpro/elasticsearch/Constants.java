package nl.vpro.elasticsearch;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
public class Constants {

    public static class Fields {

        public static final String TYPE    = "_type";
        public static final String ID      = "_id";
        public static final String INDEX   = "_index";
        public static final String ROUTING = "_routing";
        public static final String PARENT  = "_parent";
        public static final String SOURCE  = "_source";
        public static final String VERSION  = "_version";

    }


    public static final String SCORE = "_score";

    public static final String HITS = "hits";

    public static final String _SCROLL_ID = "_scroll_id";
    public static final String SCROLL_ID = "scroll_id";

    public static final String SCROLL = "scroll";

    public static final String QUERY = "query";

    @Deprecated
    public static final String TYPE = "type";
    public static final String ID  = "id";
    public static final String PARENT  = "parent";

    public static final String INDEX = "index";

    public static final String DOC = "_doc";



}
