package nl.vpro.jackson2;

import javassist.*;
import javassist.bytecode.*;
import javassist.bytecode.annotation.Annotation;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;

import javax.xml.bind.Unmarshaller;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * This can be used on a class as a json deserializer, and then jaxb's 'afterUnmarshal' will also be supported.
 *
 * This is implemented by extending the class with an override for {@link JsonDeserialize} and than deserialize that (to avoid infinite recursion), and then call the first one that exists of:
 * <ol>
 *     <li>{@code afterUnmarshal(DeserializationContext ctxt, Object parent)} Jackson specific</li>
 *     <li>{@code afterUnmarshal(Object parent)} We can't provide a {@link Unmarshaller} any way</li>
 *     <li>{@code afterUnmarshal(Unmarshaller unmarshaller, Object parent)} This would be called by jaxb. We would too, but will have to supply <code>null</code> for the Unmarshaller.</li>
 * </ol>
 *
 * So, in cases that deserialization context is needed you may need to implement 'afterUnmarshal' seperately for jaxb and jackson. If not then you can implement the version without the concerning parameter (but for jaxb you'd still have to delegate its version to that).
 *
 * E.g.
 * <pre>
 * {@code
 *     @JsonDeserialize(using = AfterUnmarshalDeserializer.class)
 *     @XmlAccessorType(XmlAccessType.NONE)
 *     static class A {
 *
 *         private final int instance = instances++;
 *
 *         private boolean unmarshalled = false;
 *
 *         Object parent;
 *
 *         void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
 *             unmarshalled = true;
 *             this.parent = parent;
 *             log.info("Calling afterUnmarshal on this {} {}", this, parent);
 *         }
 *
 *         public String toString () {
 *             return "A:" + instance;
 *         }
 *
 *     }
 * }
 * </pre>
 *
 * In the case of custom deserializer which you don't want to extends from this, it is possible to call static methods.
 *
 * <pre>
 * {@code
 *      static class FDeserializer extends JsonDeserializer<F> {
 *
 *         @Override
 *         public F deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
 *             F f = new F();
 *             Object parent = AfterUnmarshalDeserializer.getParent(p);
 *             ObjectNode on = p.readValueAsTree();
 *             f.value = on.get("falue").textValue();
 *             AfterUnmarshalDeserializer.invokeAfterUnmarshal(ctxt, f, parent);
 *             return f;
 *         }
 *     }
 *
 * }
 * </pre>
 *
 * @author Michiel Meeuwissen
 * @since 2.16
 */
@Slf4j
public class AfterUnmarshalDeserializer extends StdDeserializer<Object> implements ContextualDeserializer {
    private static final String AFTER_UNMARSHAL = "afterUnmarshal";
    private static final String CLASS_POSTFIX = "$$DefaultJsonDeserialize";
    private static ClassPool POOL;


    public AfterUnmarshalDeserializer() {
        super(Object.class);
    }
    protected AfterUnmarshalDeserializer(JavaType clazz) {
        super(clazz);
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        Object parent = getParent(p);
        Object obj = deserializeObject(p, ctxt);
        invokeAfterUnmarshal(ctxt, obj, parent);
        return obj;
    }

    protected Object deserializeObject(JsonParser p, DeserializationContext ctxt) throws IOException {
        Object deserialized = p.getCodec().treeToValue(ctxt.readTree(p), extension(_valueClass)); // we need to extend the class, otherwise this will infinitely recurse
        //Object value = ctxt.getTypeFactory().constructType(_valueType);

        return deserialized;
    }

    /**
     * Given the current {@link JsonParser} determins the parent object.
     */
    public static Object getParent(JsonParser p) {
        JsonStreamContext parentStreamContext = p.getParsingContext().getParent();
        Object parent = null;
        if (parentStreamContext != null) {
            parent = parentStreamContext.getCurrentValue();
            if (parent instanceof Collection) {
                if (parentStreamContext.getParent() != null) {
                    parent = parentStreamContext.getParent().getCurrentValue();
                }
            }
        }
        return parent;
    }

    /**
     * If parent determined, and object created, call the afterUnmarshal method.
     *
     * @exception NoSuchMethodException If no afterUnmarshal method could be found
     * @exception JsonProcessingException If exception coming for calling this method
     */

    @SuppressWarnings("JavaDoc")
    public static void invokeAfterUnmarshal(DeserializationContext cxt, Object obj, Object parent) throws JsonProcessingException {
        try {
            Class<?> valueClass = obj.getClass();
            try {
                getAfterUnmarshal(valueClass, DeserializationContext.class, Object.class).invoke(obj, cxt, parent);
            } catch (NoSuchMethodException nsfme) {
                try {
                    getAfterUnmarshal(valueClass, Object.class).invoke(obj, parent);
                } catch (NoSuchMethodException nsme) {
                    getAfterUnmarshal(valueClass, Unmarshaller.class, Object.class).invoke(obj, null, parent);
                }
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new JsonProcessingException(e) {};
        }

    }

    private static Method getAfterUnmarshal(Class<?> clazz, Class<?>... args) throws NoSuchMethodException {
        while(true) {
            try {
                Method m = clazz.getDeclaredMethod(AFTER_UNMARSHAL, args);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException nsme) {
                clazz = clazz.getSuperclass();
                if (clazz == Object.class) {
                    throw nsme;
                }
            }
        }
    }

    /**
     * extends the class (using javassist) to disable the current json deserialize class
     */
    @SneakyThrows
    public static Class<?> extension(Class<?> valueClass) {
        final String name = valueClass.getName() + CLASS_POSTFIX;
        if (POOL == null) {
            POOL = ClassPool.getDefault();
            //POOL.appendClassPath(new LoaderClassPath(Thread.currentThread().getContextClassLoader()));
            //POOL.appendClassPath(new LoaderClassPath(AfterUnmarshalDeserializer.class.getClassLoader()));
            POOL.insertClassPath(new ClassClassPath(AfterUnmarshalDeserializer.class));


        }
        CtClass copy = POOL.getOrNull(name);
        if (copy == null) {
            CtClass ctClass = POOL.getCtClass(valueClass.getName());
            copy = POOL.makeClass(name, ctClass);
            ClassFile classFile = copy.getClassFile();
            ConstPool constpool = classFile.getConstPool();

            AnnotationsAttribute annotationsAttribute = new AnnotationsAttribute(constpool, AnnotationsAttribute.visibleTag);

            Annotation annotation = new Annotation(JsonDeserialize.class.getName(), constpool);

            // it seems that the converter is called also if not present in extension, so never mind:
            //JsonDeserialize current = valueClass.getAnnotation(JsonDeserialize.class);
            //annotation.addMemberValue("converter", new ClassMemberValue(current.converter().getName(), constpool));

            annotationsAttribute.setAnnotation(annotation);

            classFile.addAttribute(annotationsAttribute);

            //copy.writeFile(System.getProperty("java.io.tmpdir"));
            copy.toBytecode();

            Class<?> clazz = copy.toClass(AfterUnmarshalDeserializer.class.getClassLoader(),
                AfterUnmarshalDeserializer.class.getProtectionDomain());
            log.info("Created {}", clazz);
            return clazz;
        } else {
            return AfterUnmarshalDeserializer.class.getClassLoader().loadClass(name);
        }
    }

    @Override
    public AfterUnmarshalDeserializer createContextual(DeserializationContext ctxt, BeanProperty property) {
        return new AfterUnmarshalDeserializer(ctxt.getContextualType());
    }
}
