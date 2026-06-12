package nl.vpro.hibernate;

import java.io.Serial;
import java.net.URI;

import org.hibernate.metamodel.mapping.DiscriminatorConverter;
import org.hibernate.metamodel.mapping.DiscriminatorType;
import org.hibernate.metamodel.mapping.internal.UnifiedAnyDiscriminatorConverter;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;

/**
 * A type that maps between {@link java.sql.Types#VARCHAR VARCHAR} and {@link java.net.URI}
 *
 * @author Michiel Meeuwissen
 */
public class URIType extends AbstractSingleColumnStandardBasicType<URI> implements DiscriminatorType<URI> {


    @Serial
    private static final long serialVersionUID = 1L;

    public URIType() {
        super(VarcharJdbcType.INSTANCE, URITypeDescriptor.INSTANCE);
    }

    @Override
    public String getName() {
        return "uri";
    }

    @Override
    public DiscriminatorConverter<URI, CharSequence> getValueConverter() {
        return new UnifiedAnyDiscriminatorConverter<URI, CharSequence>(null, null, null, null, null, null) {
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
