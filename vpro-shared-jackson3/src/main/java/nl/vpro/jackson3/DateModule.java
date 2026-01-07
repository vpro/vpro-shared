package nl.vpro.jackson3;

import tools.jackson.core.Version;
import tools.jackson.databind.module.SimpleModule;

import java.io.Serial;
import java.time.*;
import java.util.Date;


/**
 * Work around for <a href="http://jira.codehaus.org/browse/JACKSON-920">JACKSON-920</a>
 * @author Michiel Meeuwissen
 * @since 0.21
 */
public class DateModule extends SimpleModule {

    public static final ZoneId ZONE = ZoneId.of("Europe/Amsterdam");


    @Serial
    private static final long serialVersionUID = 1L;

    public DateModule() {
        super(new Version(0, 31, 0, "", "nl.vpro.shared", "vpro-jackson3"));

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
