/*
 * Copyright (C) 2007/2008 All rights reserved
 * VPRO Omroepvereniging, The Netherlands
 * Creation date 18 sep 2008
 */

package nl.vpro.domain;

import java.util.List;

import nl.vpro.domain.DomainObject;

/**
 * Superinterface for all data access objects.
 *
 * @param <T> the entity type of this dao
 * @param <PK> the primary key for this element
 */
public interface Dao<DO extends DomainObject> {

    /**
     * Retrieves a fully initialized entity by its id.
     *
     * @param id the id of the entity to return
     * @return the domainobject with the given id
     */
    DO getByID(Long id);

    /**
     * Retrieves a list of domainobjects belonging to this DAO,
     * related to an a source with id id.
     *
     * @param id the id of source source object
     * @return a list of domainobjects related to this id
     */
    List<DO> getRelatedByID(Long id);
}
