/*
 * Copyright (C) 2008 All rights reserved
 * VPRO The Netherlands
 * Creation date 18 sep 2008.
 */
package nl.vpro.domain;


import java.util.List;
import java.util.SortedSet;

import org.apache.log4j.Logger;
import org.springframework.orm.ibatis.SqlMapClientTemplate;

import com.ibatis.sqlmap.client.event.RowHandler;

import nl.vpro.domain.DomainObject;

/**
 * @author roekoe
 *
 * @param <DO>
 */
public abstract class DaoIBatisImpl<DO extends DomainObject> implements Dao<DO> {
    private static final Logger log = Logger.getLogger(DaoIBatisImpl.class);

    private SqlMapClientTemplate sqlMapClientTemplate;

    private String sqlMapNameSpace = "DomainObject";

    /**
     * Subclasses should override this constructor.  All subclasses of this class should
     * call the only public constructor in order set the SqlMap namespace.
     * to set
     *
     * @param sqlMapQlientTemplate an SqlMap template to be set when instantiated
     */
    DaoIBatisImpl(SqlMapClientTemplate sqlMapClientTemplate) {
        this.sqlMapClientTemplate = sqlMapClientTemplate;
    }

    /**
     * Default public constructor. Subclasses of this class should
     * call this public constructor in order set the SqlMapClientTemplate and
     * the SqlMap namespace.
     *
     * @param sqlMapQlientTemplate a Spring SqlMapClientTemplate to be set when instantiated
     * @param sqlMapNameSpace the SqlMap namespace as declared in the sqlmap file for this
     * DAO subclasses.
     */
    public DaoIBatisImpl(SqlMapClientTemplate sqlMapClientTemplate, String sqlMapNameSpace) {
        this.sqlMapClientTemplate = sqlMapClientTemplate;
        this.sqlMapNameSpace = sqlMapNameSpace;
    }

    /** (non-Javadoc)
     * @see nl.vpro.domain.Dao#getByID(java.lang.Long)
     */
    @SuppressWarnings("unchecked")
    public DO getByID(Long id) {
        return (DO) sqlMapClientTemplate.queryForObject(sqlMapNameSpace + ".getByID", id);
    }

    /** (non-Javadoc)
     * @see nl.vpro.domain.Dao#getRelatedById(java.lang.Long)
     */
    @SuppressWarnings("unchecked")
    public List<DO> getRelatedByID(Long id) {
        return sqlMapClientTemplate.queryForList(sqlMapNameSpace + ".getRelatedByID", id);
    }

    public void getRelatedByID(Long id, RowHandler rowHandler) {
        sqlMapClientTemplate.queryWithRowHandler(sqlMapNameSpace + ".getRelatedByID", id, rowHandler);
    }

    /**
     * @return the sqlMapClientTemplate
     */
    public SqlMapClientTemplate getSqlMapClientTemplate() {
        return sqlMapClientTemplate;
    }

    /**
     * @param sqlMapClientTemplate the sqlMapClientTemplate to set
     */
    public void setSqlMapClientTemplate(SqlMapClientTemplate sqlMapClientTemplate) {
        this.sqlMapClientTemplate = sqlMapClientTemplate;
    }

    /**
     * @return the sqlmap namespace in use
     */
    public String getSqlMapNameSpace() {
        return sqlMapNameSpace;
    }

    /**
     * @param sqlMapNameSpace the sqlmap namespace to use
     */
    public void setSqlMapNameSpace(String sqlMapNameSpace) {
        this.sqlMapNameSpace = sqlMapNameSpace;
    }

}
