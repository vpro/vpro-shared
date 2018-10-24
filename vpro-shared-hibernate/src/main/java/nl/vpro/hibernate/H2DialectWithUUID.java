/*
 * Copyright (C) 2018 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.hibernate;

import org.hibernate.dialect.H2Dialect;

import java.sql.Types;

/**
 * @author r.jansen
 */
public class H2DialectWithUUID extends H2Dialect {
    public H2DialectWithUUID() {
        super();
        registerColumnType(Types.OTHER, "uuid");
    }
}