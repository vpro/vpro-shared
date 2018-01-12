package nl.vpro.jackson2;

import org.apache.avro.specific.SpecificRecordBase;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * This modules makes avro-objects be serialized without their schema-information.
 *
 * @author Michiel Meeuwissen
 * @since 0.24
 */
public class SerializeAvroModule extends SimpleModule {

    private static final long serialVersionUID = 1L;

    public SerializeAvroModule() {
        super(new Version(0, 21, 0, "", "nl.vpro.shared", "vpro-jackson2"));

        // first deserializers

        // then serializers:
        addSerializer(SpecificRecordBase.class, Avro.Serializer.INSTANCE);

    }
}
