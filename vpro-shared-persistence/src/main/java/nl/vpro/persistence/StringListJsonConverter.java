package nl.vpro.persistence;

import lombok.SneakyThrows;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts a {@code List<String>} to/from a JSON string, for entity fields backed by a MariaDB {@code json}
 * column. Order is preserved, both ways.
 * @since 5.19
 */
@Converter
public class StringListJsonConverter implements AttributeConverter<List<String>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> TYPE = new TypeReference<>() {
    };

    @Override
    @SneakyThrows
    public String convertToDatabaseColumn(List<String> attribute) {
        if (attribute == null) {
            return null;
        }
        return MAPPER.writeValueAsString(attribute);

    }

    @Override
    @SneakyThrows
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return MAPPER.readValue(dbData, TYPE);

    }
}
