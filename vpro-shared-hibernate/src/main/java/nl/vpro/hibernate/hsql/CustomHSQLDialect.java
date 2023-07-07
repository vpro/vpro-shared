/*
 * Copyright (C) 2015 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.hibernate.hsql;

import java.io.Serial;
import java.util.UUID;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.java.UUIDTypeDescriptor;
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor;

/**
 *
 * @author Roelof Jan Koekoek
 * @since 2.6
 */
public class CustomHSQLDialect extends HSQLDialect {

    @Override
    public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
        super.contributeTypes(typeContributions, serviceRegistry);

        typeContributions.contributeType(new UUIDType());
    }

    public static class UUIDType extends AbstractSingleColumnStandardBasicType<UUID> {

        @Serial
        private static final long serialVersionUID = -7244515432254939427L;

        public UUIDType() {
            super(VarcharTypeDescriptor.INSTANCE, UUIDTypeDescriptor.INSTANCE);
        }

        @Override
        public String getName() {
            return "pg-uuid";
        }
    }
}
