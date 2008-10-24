/*
 * Copyright (C) 2008 All rights reserved
 * VPRO The Netherlands
 * Creation date 18 sep 2008.
 */
package nl.vpro.domain;


import org.apache.log4j.Logger;
import org.springframework.orm.ibatis.SqlMapClientTemplate;

import nl.vpro.domain.DomainObject;

/**
 * @author roekoe
 *
 */
public abstract class DaoIBatisImpl<DO extends DomainObject> implements Dao<DO> {
    private static final Logger log = Logger.getLogger(DaoIBatisImpl.class);

    private SqlMapClientTemplate sqlMapClientTemplate;

    /**
     * @param sqlMapClientTemplate
     */
    public DaoIBatisImpl(SqlMapClientTemplate sqlMapClientTemplate) {
        this.sqlMapClientTemplate = sqlMapClientTemplate;
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

}
