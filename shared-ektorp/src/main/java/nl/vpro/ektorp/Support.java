package nl.vpro.ektorp;

import org.ektorp.CouchDbConnector;
import org.ektorp.DbAccessException;
import org.ektorp.impl.NameConventions;
import org.ektorp.support.CouchDbRepositorySupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Michiel Meeuwissen
 * @since 3.6
 */
public class Support<T> extends CouchDbRepositorySupport<T> {
    private static final Logger LOG = LoggerFactory.getLogger(Support.class);


    public static <T> Support<T> getInstance(String logInfo, Class<T> type, CouchDbConnector db, boolean createIfNotExists) {
        try {
            return new Support<>(type, db, createIfNotExists);
        } catch (DbAccessException dbe) {
            LOG.error(logInfo + " " + db.getDatabaseName() + ": " + dbe.getClass().getName() + " " + dbe.getMessage());
            LOG.info("Will create a connector to couchdb without trying to check and create the database (supposing it is temporary down now");
            return new Support<>(type, db, false);
        }
    }

    public static <T> Support<T> getInstance(String logInfo, Class<T> type, CouchDbConnector db) {
        return getInstance(logInfo, type, db, true);
    }


    protected Support(Class<T> type, CouchDbConnector db) {
        super(type, db);
    }

    protected Support(Class<T> type, CouchDbConnector db, boolean createIfNotExists) {
        super(type, db, createIfNotExists);
    }

    public CouchDbConnector getConnector() {
        return db;
    }

    public String getPath() {
        return getConnector().path();
    }

    public String getDesignDocumentId() {
        return NameConventions.designDocName(type);
    }

}
