package nl.vpro.jackson2;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;

import nl.vpro.util.IterableWrapper;

/**
 * Like {@link IterableJson}, but for {@link IterableWrapper}s.
 * <p>
 * A wrapper with exactly one element is serialized as just that element, otherwise as an array. Both forms are accepted when deserializing.
 *
 * @author Michiel Meeuwissen
 * @since 5.20.3
 */
public class IterableWrapperJson {

    private IterableWrapperJson() {
    }

    private static final Set<Class<?>> SIMPLE_TYPES = Set.of(String.class, Character.class, Boolean.class, Integer.class, Float.class, Long.class, Double.class);

    public static class Serializer extends JsonSerializer<IterableWrapper<?>> {

        @Override
        public void serialize(IterableWrapper<?> value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            if (value == null) {
                jgen.writeNull();
                return;
            }
            List<?> list = value.asList();
            if (list.size() == 1) {
                jgen.writeObject(list.get(0));
            } else {
                jgen.writeStartArray();
                for (Object o : list) {
                    jgen.writeObject(o);
                }
                jgen.writeEndArray();
            }
        }
    }

    /**
     * Extend this, and provide a no-args constructor calling {@link #Deserializer(Function, Class)}.
     * @param <L> The {@link IterableWrapper} implementation
     * @param <T> The type of its elements
     */
    public static abstract class Deserializer<L extends IterableWrapper<T>, T> extends JsonDeserializer<L> {

        private final Function<List<T>, L> creator;

        private final Class<T> memberClass;

        private final boolean isSimple;

        protected Deserializer(Function<List<T>, L> creator, Class<T> memberClass) {
            this.creator = creator;
            this.memberClass = memberClass;
            this.isSimple = memberClass.isPrimitive() || SIMPLE_TYPES.contains(memberClass);
        }

        @Override
        public L deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            if (jp.getParsingContext().inObject()) {
                if (!isSimple) {
                    jp.clearCurrentToken();
                }
                T rs = jp.readValueAs(memberClass);
                return creator.apply(new ArrayList<>(Collections.singletonList(rs)));
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
