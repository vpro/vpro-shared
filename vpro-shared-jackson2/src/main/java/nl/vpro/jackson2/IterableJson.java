package nl.vpro.jackson2;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * @author Michiel Meeuwissen
 * @since 0.32
 */
public class IterableJson {


    public static class Serializer extends JsonSerializer<Iterable<?>> {
        @Override
        public void serialize(Iterable value, JsonGenerator jgen, SerializerProvider provider) throws IOException {

            if (value == null) {
                jgen.writeNull();
            } else {
                Iterator<?> i = value.iterator();
                Object v;
                if (i.hasNext()) {
                    v = i.next();
                    if (! i.hasNext()) {
                        jgen.writeObject(v);
                    } else {
                        jgen.writeStartArray();
                        jgen.writeObject(v);
                        while (i.hasNext()) {
                            jgen.writeObject(i.next());
                        }
                        jgen.writeEndArray();
                    }
                } else {
                    jgen.writeStartArray();
                    jgen.writeEndArray();
                }
            }
        }
    }

    private static final Set<Class> simpleTypes = new HashSet<>(Arrays.asList(String.class, Character.class, Boolean.class, Integer.class, Float.class, Long.class, Double.class));
    public static abstract class Deserializer<T> extends JsonDeserializer<Iterable<T>> {

        private final Function<List<T>, Iterable<T>> creator;

        private final Class<T> memberClass;

        private final boolean isSimple;


        public Deserializer(Function<List<T>, Iterable<T>> supplier, Class<T> memberClass) {
            this.creator = supplier;
            this.memberClass = memberClass;
            isSimple = memberClass.isPrimitive() || simpleTypes.contains(memberClass);
        }

        @SuppressWarnings("unchecked")
        public Deserializer(Function<List<T>, Iterable<T>> supplier) {
            this.creator = supplier;
            try {
                this.memberClass = (Class<T>) supplier.getClass().getMethod("apply").getReturnType().getGenericSuperclass();
            } catch (NoSuchMethodException e) {
                throw new RuntimeException();
            }
            isSimple = memberClass.isPrimitive() || simpleTypes.contains(memberClass);
        }


        @Override
        public Iterable<T> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            if (jp.getParsingContext().inObject()) {
                if (! isSimple) {
                    jp.clearCurrentToken();
                }
                T rs = jp.readValueAs(memberClass);
                return creator.apply(Collections.singletonList(rs));
            } else if (jp.getParsingContext().inArray()) {
                List<T> list = new ArrayList<>();
                jp.clearCurrentToken();
                Iterator<T> i = jp.readValuesAs(memberClass);
                while (i.hasNext()) {
                    list.add(i.next());
                }
                return creator.apply(list);
            } else {
                throw new IllegalStateException();
            }
        }
    }
}
