package nl.vpro.persistence;

import lombok.SneakyThrows;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts a {@code Map<String, String>} to/from a JSON string, for entity fields backed by a MariaDB {@code json}
 * column.
 * @since 5.19
 */
@Converter
public class StringMapJsonConverter implements AttributeConverter<Map<String, String>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, String>> TYPE = new TypeReference<>() {
    };

    @Override
    @SneakyThrows
    public String convertToDatabaseColumn(Map<String, String> attribute) {
        if (attribute == null) {
            return null;
        }
        return MAPPER.writeValueAsString(attribute);

    }

    @Override
    @SneakyThrows
    public Map<String, String> convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return MAPPER.readValue(dbData, TYPE);

    }
}
