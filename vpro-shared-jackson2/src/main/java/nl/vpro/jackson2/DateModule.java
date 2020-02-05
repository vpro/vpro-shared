package nl.vpro.jackson2;

import java.time.*;
import java.util.Date;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;


/**
 * Work around for http://jira.codehaus.org/browse/JACKSON-920
 * @author Michiel Meeuwissen
 * @since 0.21
 */
public class DateModule extends SimpleModule {

    public static final ZoneId ZONE = ZoneId.of("Europe/Amsterdam");


    private static final long serialVersionUID = 1L;

    public DateModule() {
        super(new Version(0, 31, 0, "", "nl.vpro.shared", "vpro-jackson2"));

        // first deserializers
        addDeserializer(Date.class, DateToJsonTimestamp.Deserializer.INSTANCE);
        addDeserializer(Instant.class, InstantToJsonTimestamp.Deserializer.INSTANCE);
        addDeserializer(ZonedDateTime.class, ZonedDateTimeToJsonTimestamp.Deserializer.INSTANCE);
        addDeserializer(LocalDate.class, LocalDateToJsonDate.Deserializer.INSTANCE);
        addDeserializer(Duration.class, DurationToJsonTimestamp.Deserializer.INSTANCE);


        // then serializers:
        addSerializer(Date.class, DateToJsonTimestamp.Serializer.INSTANCE);
        addSerializer(Instant.class, InstantToJsonTimestamp.Serializer.INSTANCE);
        addSerializer(ZonedDateTime.class, ZonedDateTimeToJsonTimestamp.Serializer.INSTANCE);
        addSerializer(LocalDate.class, LocalDateToJsonDate.Serializer.INSTANCE);
        addSerializer(Duration.class, DurationToJsonTimestamp.Serializer.INSTANCE);


    }
}
