/*
 * Copyright (C) 2008 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.transfer.json;

/**
 * Defines the base properties for a JSONDocument. Root entities (documents) to be published
 * to JSON should implement this interface.
 *
 * @author r.j.koekoek@vpro.nl
 */
public interface JSONDocument {
    String getType();

    String getVersion();

    String getId();

}

