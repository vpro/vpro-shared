package nl.vpro.hibernate;

import org.hibernate.metamodel.mapping.*;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;

import java.net.URI;

/**
 * A type that maps between {@link java.sql.Types#VARCHAR VARCHAR} and {@link java.net.URI}
 *
 * @author Michiel Meeuwissen
 */
public class URIType extends AbstractSingleColumnStandardBasicType<URI> implements DiscriminatorType<URI> {

    public URIType() {
        super(VarcharJdbcType.INSTANCE, URITypeDescriptor.INSTANCE);
    }

    @Override
    public String getName() {
        return "uri";
    }

    @Override
    public DiscriminatorConverter<URI, CharSequence> getValueConverter() {
        return new MappedDiscriminatorConverter<URI, CharSequence>(null, null, null, null) {
            @Override
            public URI toDomainValue(CharSequence relationalValue) {
                return URI.create((String) relationalValue);
            }

            @Override
            public CharSequence toRelationalValue(URI domainValue) {
                return domainValue.toString();
            }
        };
    }

    @Override
    public BasicType<?> getUnderlyingJdbcMapping() {
        return new URIType();
    }

    @Override
    protected boolean registerUnderJavaType() {
        return true;
    }


}
