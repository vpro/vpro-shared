/*
 * Copyright (C) 2018 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.hibernate.h2;

import java.sql.Types;

import org.hibernate.dialect.H2Dialect;

/**
 * @author r.jansen
 */
public class H2DialectWithUUID extends H2Dialect {
    public H2DialectWithUUID() {
        super();
        registerColumnType(Types.OTHER, "uuid");
    }
}
