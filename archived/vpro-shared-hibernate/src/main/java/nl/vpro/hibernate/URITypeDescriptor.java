package nl.vpro.hibernate;

import org.hibernate.HibernateException;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractClassJavaType;

import java.net.URI;
import java.net.URISyntaxException;


/**
 * Descriptor for {@link java.net.URI} handling.
 *
 * @author Michiel Meeuwissen
 */
class URITypeDescriptor extends AbstractClassJavaType<URI> {
    public static final URITypeDescriptor INSTANCE = new URITypeDescriptor(URI.class);

    protected URITypeDescriptor(Class<? extends URI> type) {
        super(type);
    }


    @Override
    public String toString(URI value) {
        return value.toString();
    }

    @Override
    public URI fromString(CharSequence string) {
        try {
            return new URI(string.toString());
        } catch (URISyntaxException e) {
            throw new HibernateException("Unable to convert string [" + string + "] to URI : " + e);
        }
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public <X> X unwrap(URI value, Class<X> type, WrapperOptions options) {
        if (value == null) {
            return null;
        }
        if (String.class.isAssignableFrom(type)) {
            return (X) toString(value);
        }
        throw unknownUnwrap(type);
    }

    @Override
    public <X> URI wrap(X value, WrapperOptions options) {
        if (value == null) {
            return null;
        }
        if (value instanceof CharSequence s) {
            return fromString(s);
        }
        throw unknownWrap(value.getClass());
    }
}
