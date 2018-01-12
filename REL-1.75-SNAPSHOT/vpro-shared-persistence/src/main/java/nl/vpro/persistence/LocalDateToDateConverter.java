package nl.vpro.persistence;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * @author Michiel Meeuwissen
 * @since 0.36
 */
@Converter
public class LocalDateToDateConverter implements AttributeConverter<LocalDate, Date> {

    public static ZoneId ZONE_ID = ZoneId.of("Europe/Amsterdam");


    @Override
    public Date convertToDatabaseColumn(LocalDate localDate) {
        return localDate == null ? null : Date.valueOf(localDate);
    }

    @Override
    public LocalDate convertToEntityAttribute(Date date) {
        return date == null ? null : date.toLocalDate();
    }
}
