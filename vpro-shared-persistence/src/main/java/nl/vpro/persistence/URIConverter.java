package nl.vpro.persistence;

import java.net.URI;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * @author Michiel Meeuwissen
 * @since 2.8
 */
@Converter
public class URIConverter implements AttributeConverter<URI, String> {

    @Override
    public String convertToDatabaseColumn(URI uri) {
        return uri == null ? null : uri.toString();
    }

    @Override
    public URI convertToEntityAttribute(String uri) {
        return uri == null ? null : URI.create(uri);
    }
}
