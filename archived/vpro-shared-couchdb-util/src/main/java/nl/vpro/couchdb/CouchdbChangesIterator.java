/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.couchdb;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
public class CouchdbChangesIterator extends CouchdbStreamIterator {

    public CouchdbChangesIterator(InputStream stream) throws IOException {
        super(stream, 3, "results");
    }

    @Override
    public String toString() {
        return super.toString() + " changes";
    }
}
