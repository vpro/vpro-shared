/*
 * Copyright (C) 2015 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.hibernate.hsql;

import java.util.UUID;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.java.UUIDTypeDescriptor;
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor;

/**
 * @author Roelof Jan Koekoek
 * @since 3.7
 */
public class CustomHSQLDialect extends HSQLDialect {

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
