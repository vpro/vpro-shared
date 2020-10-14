
package nl.vpro.jackson2;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.std.DelegatingDeserializer;

/**
 * @author Michiel Meeuwissen
 */
@Slf4j
public class AfterUnmarshalModifier extends BeanDeserializerModifier {

    @Override
    public JsonDeserializer<?> modifyDeserializer(
        DeserializationConfig config,
        BeanDescription beanDesc,
        final JsonDeserializer<?> deserializer) {
        return new AfterUnmarshalModifierDeserializer(deserializer);
    }

    static class AfterUnmarshalModifierDeserializer extends DelegatingDeserializer {

        private final JsonDeserializer<?> deserializer;

        final Deque<List<Object>> childs = new ArrayDeque<>();

        public AfterUnmarshalModifierDeserializer(JsonDeserializer<?> deserializer) {
            super(deserializer);
            this.deserializer = deserializer;
        }

        @Override
        protected JsonDeserializer<?> newDelegatingInstance(JsonDeserializer<?> newDelegatee) {
            return deserializer;
        }

        @Override
        public Object deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {

            List<Object> parent = childs.peek();
           ;
            childs.push(new ArrayList<>());
            log.info("push {} {}", childs.hashCode(), childs.size());
            Object result = _delegatee.deserialize(jp, ctxt);
            if (parent != null) {
                parent.add(result);
            }
            log.info("pop");
            List<Object> foundChilds = childs.pop();
            log.info("" + result + ":" + childs);

            try {
                boolean invoked = invokeAfterUnmarshal(ctxt, result, null);

            } catch (Exception e) {
                log.debug(e.getMessage());

            }

            return result;
        }



    }



    /**
     * If parent determined, and object created, call the afterUnmarshal method.
     *
     * @exception JsonProcessingException If exception coming for calling this method
     */
    static boolean invokeAfterUnmarshal(DeserializationContext cxt, Object obj, Object parent) throws JsonProcessingException {
        Class<?> valueClass = obj.getClass();
        Optional<AfterUnmarshalWrapper> afterUnmarshal = getAfterUnmarshal(valueClass);
        if (afterUnmarshal.isPresent()) {
            afterUnmarshal.get().afterUnmarshal(obj, cxt, parent);
            return true;
        } else {
            return false;
        }
    }

    private static final Map<Class<?>, Optional<AfterUnmarshalWrapper>> cache = new ConcurrentHashMap<>(); // speeds  up about 20 times or so.


    private static Optional<AfterUnmarshalWrapper> getAfterUnmarshal(Class<?> clazz)  {
        return cache.computeIfAbsent(clazz, AfterUnmarshalModifier::getAfterUnmarshalUncached);
        //return getAfterUnmarshalUncached(clazz);
    }


    private static Optional<AfterUnmarshalWrapper> getAfterUnmarshalUncached(Class<?> clazz)  {
        Method method = null;
        do {
            for (Method m : clazz.getDeclaredMethods()) {
                if (m.getAnnotation(AfterUnmarshal.class) != null) {
                    m.setAccessible(true);
                    method = m;
                    break;
                }
            }
            clazz = clazz.getSuperclass();
        } while (clazz != Object.class);
        if (method == null) {
            return Optional.empty();
        } else {
            final int parameterCount = method.getParameterCount();
            int deserializationContextPos = -1;
            int parentPos = -1;
            for (int i = 0; i < parameterCount; i++) {
                if (DeserializationContext.class.isAssignableFrom(method.getParameters()[i].getType())) {
                    if (deserializationContextPos == -1) {
                        deserializationContextPos = i;
                    }
                } else if (parentPos == -1) {
                    parentPos = i;
                }
            }
            final int finalDeserializationContextPos = deserializationContextPos;
            final int finalParentPos = parentPos;
            final Method finalMethod = method;
            return Optional.of((object, deserializationContext, parent) -> {
                Object[] args = new Object[parameterCount];
                if (finalDeserializationContextPos != -1) {
                    args[finalDeserializationContextPos] = deserializationContext;
                }
                if (finalParentPos != -1){
                    args[finalParentPos] = parent;
                }
                try {
                    finalMethod.invoke(object, args);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            });
        }

    }


    @FunctionalInterface
    private  interface AfterUnmarshalWrapper {

        void afterUnmarshal(Object object, DeserializationContext deserializationConfig, Object parent);

    }

}
