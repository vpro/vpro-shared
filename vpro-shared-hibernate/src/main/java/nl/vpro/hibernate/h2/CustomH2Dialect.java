/*
 * Copyright (C) 2015 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.hibernate.h2;

import java.util.UUID;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.java.UUIDTypeDescriptor;
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor;

/**
 * @author Michiel Meeuwissen
 * @since 2.6
 */
public class CustomH2Dialect extends H2Dialect {

    @Override
    public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
        super.contributeTypes(typeContributions, serviceRegistry);

        typeContributions.contributeType(new UUIDType());
    }

    public class UUIDType extends AbstractSingleColumnStandardBasicType<UUID> {

        public UUIDType() {
            super(VarcharTypeDescriptor.INSTANCE, UUIDTypeDescriptor.INSTANCE);
        }

        @Override
        public String getName() {
            return "pg-uuid";
        }
    }
}
