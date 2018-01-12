package nl.vpro.couchdb;

/**
 * @author Michiel Meeuwissen
 * @since 1.0
 */
public class CouchdbView {

    private final String documentName;
    private final String viewName;

    public CouchdbView(String documentName, String viewName) {
        this.documentName = documentName;
        this.viewName = viewName;
    }

    @Override
    public String toString() {
        return "_design/" + documentName + "/_view/" + viewName;
    }
}
