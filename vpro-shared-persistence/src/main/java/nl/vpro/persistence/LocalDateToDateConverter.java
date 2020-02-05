package nl.vpro.persistence;

import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * @author Michiel Meeuwissen
 * @since 0.36
 */
@Converter
public class LocalDateToDateConverter implements AttributeConverter<LocalDate, Date> {

    public static final ZoneId ZONE_ID = ZoneId.of("Europe/Amsterdam");

    @Override
    public Date convertToDatabaseColumn(LocalDate localDate) {
        return localDate == null ? null : Date.valueOf(localDate);
    }

    @Override
    public LocalDate convertToEntityAttribute(Date date) {
        return date == null ? null : date.toLocalDate();
    }
}
