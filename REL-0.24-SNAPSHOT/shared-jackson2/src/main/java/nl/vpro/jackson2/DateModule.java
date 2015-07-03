package nl.vpro.jackson2;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;


/**
 * Work around for http://jira.codehaus.org/browse/JACKSON-920
 * @author Michiel Meeuwissen
 * @since 0.21
 */
public class DateModule extends SimpleModule {

    public static ZoneId ZONE = ZoneId.of("Europe/Amsterdam");


    private static final long serialVersionUID = 1L;

    public DateModule() {
        super(new Version(0, 21, 0, "", "nl.vpro.shared", "vpro-jackson2"));

        // first deserializers
        addDeserializer(Date.class, DateToJsonTimestamp.Deserializer.INSTANCE);
        addDeserializer(Instant.class, InstantToJsonTimestamp.Deserializer.INSTANCE);
        addDeserializer(ZonedDateTime.class, ZonedDateTimeToJsonTimestamp.Deserializer.INSTANCE);


        // then serializers:
        addSerializer(Date.class, DateToJsonTimestamp.Serializer.INSTANCE);
        addSerializer(Instant.class, InstantToJsonTimestamp.Serializer.INSTANCE);
        addSerializer(ZonedDateTime.class, ZonedDateTimeToJsonTimestamp.Serializer.INSTANCE);

    }
}
