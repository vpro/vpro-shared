/*
 * Copyright (C) 2008 All rights reserved
 * VPRO The Netherlands
 * Creation date 18 sep 2008.
 */
package nl.vpro.domain;


import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.orm.ibatis.SqlMapClientTemplate;
import org.springframework.stereotype.Repository;

import com.ibatis.sqlmap.client.event.RowHandler;

import nl.vpro.domain.SharedDomainObject;

import javax.annotation.Resource;

/**
 * @author roekoe
 *
 * @param <DO>
 */
@Repository
public abstract class DaoIBatisImpl<DO extends SharedDomainObject> implements Dao<DO> {
    private static final Logger log = Logger.getLogger(DaoIBatisImpl.class);

    private SqlMapClientTemplate sqlMapClientTemplate;

    private String sqlMapNameSpace = "SharedDomainObject";

    public DaoIBatisImpl() {
    }

    @SuppressWarnings("unchecked")
    public DO getByID(Long id) {
        return (DO) sqlMapClientTemplate.queryForObject(sqlMapNameSpace + ".getByID", id);
    }

    @SuppressWarnings("unchecked")
    public List<DO> getRelatedByID(Long id) {
        return sqlMapClientTemplate.queryForList(sqlMapNameSpace + ".getRelatedByID", id);
    }

    public void getRelatedByID(Long id, RowHandler rowHandler) {
        sqlMapClientTemplate.queryWithRowHandler(sqlMapNameSpace + ".getRelatedByID", id, rowHandler);
    }

    public SqlMapClientTemplate getSqlMapClientTemplate() {
        return sqlMapClientTemplate;
    }

    @Resource(name = "mediaSqlMapClientTemplate")
    public void setSqlMapClientTemplate(SqlMapClientTemplate sqlMapClientTemplate) {
        this.sqlMapClientTemplate = sqlMapClientTemplate;
    }

    public String getSqlMapNameSpace() {
        return sqlMapNameSpace;
    }

    public void setSqlMapNameSpace(String sqlMapNameSpace) {
        this.sqlMapNameSpace = sqlMapNameSpace;
    }

}
