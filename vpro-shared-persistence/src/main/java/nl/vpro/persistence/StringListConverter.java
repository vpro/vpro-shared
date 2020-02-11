package nl.vpro.persistence;


import java.util.Arrays;
import java.util.List;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * @author Michiel Meeuwissen
 * @since 2.8
 */
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    public static final StringListConverter INSTANCE = new StringListConverter();

    private static final String SPLIT_CHAR = "\t";

    @Override
    public String convertToDatabaseColumn(List<String> stringList) {
        if (stringList == null || stringList.isEmpty()) {
            return null;
        }
        return String.join(SPLIT_CHAR, stringList);
    }

    @Override
    public List<String> convertToEntityAttribute(String string) {
        if (string == null || string.isEmpty()) {
            return null;
        }
        return Arrays.asList(string.split(SPLIT_CHAR));
    }
}
