package nl.vpro.jackson2;

import java.util.Date;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;


/**
 * @author Michiel Meeuwissen
 * @since ...
 */
public class DateModule extends SimpleModule {

    private static final long serialVersionUID = 1L;

    public DateModule() {
        super(new Version(0, 21, 0, "", "nl.vpro.shared", "vpro-jackson2"));

        // first deserializers
        addDeserializer(Date.class, DateToJsonTimestamp.Deserializer.INSTANCE);


        // then serializers:
        addSerializer(Date.class, DateToJsonTimestamp.Serializer.INSTANCE);
    }
}
