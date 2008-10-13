/*
 * Copyright (C) 2007/2008 All rights reserved
 * VPRO Omroepvereniging, The Netherlands
 * Creation date 18 sep 2008
 */

package nl.vpro.domain;

import nl.vpro.domain.DomainObject;

/**
 * Superinterface for all data access objects.
 *
 * @param <T> the entity type of this dao
 * @param <PK> the primary key for this element
 */
public interface Dao<DO extends DomainObject> {

    /**
     * Finds an entity by id.
     *
     * @param id the id of the entity to return
     * @return the entity with the given id
     */
    //void load(DO domainObject, String... paths) throws Exception;

}
