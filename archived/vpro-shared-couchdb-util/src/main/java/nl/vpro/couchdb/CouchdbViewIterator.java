package nl.vpro.couchdb;

import java.io.IOException;
import java.io.InputStream;

/**
 * An iterator over a couchdb view that is streaming, which doesn't use any memory, so you can use for huge results.
 *
 * @author Michiel Meeuwissen
 * @since 1.0
 */
public class CouchdbViewIterator extends CouchdbStreamIterator {

    public CouchdbViewIterator(InputStream stream) throws IOException {
        super(stream, 3, "rows");
    }
}
