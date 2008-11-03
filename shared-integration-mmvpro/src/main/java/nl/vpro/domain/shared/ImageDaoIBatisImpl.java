/*
 * Copyright (C) 2008 All rights reserved
 * VPRO The Netherlands
 * Creation date 18 sep 2008.
 */
package nl.vpro.domain.shared;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ibatis.SqlMapClientTemplate;
import org.springframework.stereotype.Repository;

import nl.vpro.domain.DaoIBatisImpl;

/**
 * @author roekoe
 *
 */
@Repository
public class ImageDaoIBatisImpl extends DaoIBatisImpl<Image>
        implements ImageDao {
    private static final Logger log = Logger.getLogger(ImageDaoIBatisImpl.class);

    /**
     * The sqlmap namespace as declared in the sqlmap belonging to this DAO.
     */
    private static final String SQLMAP_NAMESPACE = "Image";

    @Autowired
    ImageDaoIBatisImpl(SqlMapClientTemplate sqlMapClientTemplate) {
        super(sqlMapClientTemplate, SQLMAP_NAMESPACE);
    }
}
