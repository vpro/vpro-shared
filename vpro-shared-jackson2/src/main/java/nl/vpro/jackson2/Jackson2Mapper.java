/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.jackson2;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;
import java.util.function.Predicate;

import org.slf4j.event.Level;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import com.google.common.annotations.Beta;

import nl.vpro.logging.Slf4jHelper;

/**
 * @author Rico
 * @author Michiel Meeuwissen
 */
@Slf4j
public class Jackson2Mapper extends ObjectMapper {

    private static final long serialVersionUID = 8353430660109292010L;

    private static boolean loggedAboutAvro = false;
    private static boolean loggedAboutFallback = false;

    private static final SimpleFilterProvider FILTER_PROVIDER = new SimpleFilterProvider();


    public static final Jackson2Mapper INSTANCE = new Jackson2Mapper("instance");
    public static final Jackson2Mapper LENIENT = new Jackson2Mapper("lenient");
    public static final Jackson2Mapper STRICT = new Jackson2Mapper("strict");
    public static final Jackson2Mapper PRETTY_STRICT = new Jackson2Mapper("pretty_strict");

    public static final Jackson2Mapper PRETTY = new Jackson2Mapper("pretty");
    public static final Jackson2Mapper PUBLISHER = new Jackson2Mapper("publisher");
    public static final Jackson2Mapper PRETTY_PUBLISHER = new Jackson2Mapper("pretty_publisher");
    public static final Jackson2Mapper BACKWARDS_PUBLISHER = new Jackson2Mapper("backwards_publisher");

    private static final Jackson2Mapper MODEL = new Jackson2Mapper("model");
    private static final Jackson2Mapper MODEL_AND_NORMAL = new Jackson2Mapper("model_and_normal");



    private static final ThreadLocal<Jackson2Mapper> THREAD_LOCAL = ThreadLocal.withInitial(() -> INSTANCE);



    static {
        LENIENT.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);
        STRICT.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        PRETTY.enable(SerializationFeature.INDENT_OUTPUT);
        PRETTY_STRICT.enable(SerializationFeature.INDENT_OUTPUT);

        INSTANCE.setConfig(INSTANCE.getSerializationConfig().withView(Views.Forward.class));
        INSTANCE.setConfig(INSTANCE.getDeserializationConfig().withView(Views.Forward.class));

        LENIENT.setConfig(LENIENT.getSerializationConfig().withView(Views.Forward.class));
        LENIENT.setConfig(LENIENT.getDeserializationConfig().withView(Views.Forward.class));

        STRICT.setConfig(STRICT.getSerializationConfig().withView(Views.Forward.class));
        STRICT.setConfig(STRICT.getDeserializationConfig().withView(Views.Forward.class));


        PRETTY_STRICT.setConfig(PRETTY_STRICT.getSerializationConfig().withView(Views.Forward.class));
        PRETTY_STRICT.setConfig(PRETTY_STRICT.getDeserializationConfig().withView(Views.Forward.class));


        PUBLISHER.setConfig(PUBLISHER.getSerializationConfig().withView(Views.ForwardPublisher.class));
        PUBLISHER.setConfig(PUBLISHER.getDeserializationConfig().withView(Views.Forward.class));

        PRETTY_PUBLISHER.setConfig(PUBLISHER.getSerializationConfig().withView(Views.ForwardPublisher.class));
        PRETTY_PUBLISHER.setConfig(PUBLISHER.getDeserializationConfig().withView(Views.Forward.class));
        PRETTY_PUBLISHER.enable(SerializationFeature.INDENT_OUTPUT);

        BACKWARDS_PUBLISHER.setConfig(BACKWARDS_PUBLISHER.getSerializationConfig().withView(Views.Publisher.class));
        BACKWARDS_PUBLISHER.setConfig(BACKWARDS_PUBLISHER.getDeserializationConfig().withView(Views.Normal.class));
        BACKWARDS_PUBLISHER.enable(SerializationFeature.INDENT_OUTPUT);


        //PRETTY.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES); // This gives quite a lot of troubles. Though I'd like it to be set, especially because PRETTY is used in tests.
        PRETTY_STRICT.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES); // This gives quite a lot of troubles. Though I'd like it to be set, especially because PRETTY is used in tests.


        MODEL.setConfig(MODEL.getSerializationConfig().withView(Views.Model.class));
        MODEL_AND_NORMAL.setConfig(MODEL_AND_NORMAL.getSerializationConfig().withView(Views.ModelAndNormal.class));

    }

    public static Jackson2Mapper getInstance()  {
        return INSTANCE;
    }

    public static Jackson2Mapper getLenientInstance() {
        return LENIENT;
    }

    public static Jackson2Mapper getPrettyInstance() {
        return PRETTY;
    }

    public static Jackson2Mapper getPrettyStrictInstance() {
        return PRETTY_STRICT;
    }


    public static Jackson2Mapper getStrictInstance() {
        return STRICT;
    }

    public static Jackson2Mapper getPublisherInstance() {
        return PUBLISHER;
    }

    public static Jackson2Mapper getPrettyPublisherInstance() {
        return PRETTY_PUBLISHER;
    }

    public static Jackson2Mapper getBackwardsPublisherInstance() {
        return BACKWARDS_PUBLISHER;
    }

    @Beta
    public static Jackson2Mapper getModelInstance() {
        return MODEL;
    }

    @Beta
    public static Jackson2Mapper getModelAndNormalInstance() {
        return MODEL_AND_NORMAL;
    }

    public static Jackson2Mapper getThreadLocal() {
        return THREAD_LOCAL.get();
    }
    public static void setThreadLocal(Jackson2Mapper set) {
        if (set == null) {
            THREAD_LOCAL.remove();
        } else {
            THREAD_LOCAL.set(set);
        }
    }

    @SneakyThrows({JsonProcessingException.class})
    public static <T> T lenientTreeToValue(JsonNode jsonNode, Class<T> clazz) {
        return getLenientInstance().treeToValue(jsonNode, clazz);
    }

    private final String toString;
    private boolean inited = false;

    private Jackson2Mapper(String toString, Predicate<Module> predicate) {
        configureMapper(this, predicate);
        this.toString = toString;
    }

    private Jackson2Mapper(String toString) {
        configureMapper(this);
        this.toString = toString;
    }

    @Override
    public Jackson2Mapper setConfig(SerializationConfig config) {
        if (inited) {
            throw new IllegalStateException("Already initialized. Pleasy copy first");
        }
        return (Jackson2Mapper) super.setConfig(config);
    }

    @SafeVarargs
    public static Jackson2Mapper create(String toString, Predicate<Module> module, Consumer<ObjectMapper>... consumer) {
        Jackson2Mapper result =  new Jackson2Mapper(toString, module);
        for (Consumer<ObjectMapper> c : consumer){
            c.accept(result);
        }
        return result;
    }

    public static void configureMapper(ObjectMapper mapper) {
        configureMapper(mapper, m -> true);
    }

    public static void configureMapper(ObjectMapper mapper, Predicate<Module> filter) {
        mapper.setFilterProvider(FILTER_PROVIDER);

         AnnotationIntrospector introspector = new AnnotationIntrospectorPair(
            new JacksonAnnotationIntrospector(),
            new JaxbAnnotationIntrospector(mapper.getTypeFactory()
            ));

        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        mapper.setAnnotationIntrospector(introspector);

        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES); // This seems a good idea when reading from couchdb or so, but when reading user supplied forms, it is confusing not getting errors.

        mapper.enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES);
        mapper.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);
        mapper.enable(MapperFeature.USE_WRAPPER_NAME_AS_PROPERTY_NAME);

        try {
            // this should nbe needed, but if I don't do this, resteasy still doesn't allow comments
            mapper.enable(JsonParser.Feature.ALLOW_COMMENTS);

            mapper.setConfig(mapper.getDeserializationConfig().with(JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS));
            mapper.setConfig(mapper.getDeserializationConfig().with(JsonReadFeature.ALLOW_JAVA_COMMENTS));
        } catch (NoClassDefFoundError noClassDefFoundError) {
            Slf4jHelper.log(log,  loggedAboutFallback ? Level.DEBUG : Level.WARN, noClassDefFoundError.getMessage() + " temporary falling back. Please upgrade jackson");
            loggedAboutFallback = true;

            mapper.enable(JsonParser.Feature.ALLOW_COMMENTS);
            //noinspection deprecation
            mapper.enable(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS);

        }

        register(mapper, filter, new JavaTimeModule());
        register(mapper, filter, new DateModule());
        // For example normal support for Optional.
        Jdk8Module jdk8Module = new Jdk8Module();
        jdk8Module.configureAbsentsAsNulls(true);
        register(mapper, filter, jdk8Module);

        mapper.setConfig(mapper.getSerializationConfig().withView(Views.Normal.class));
        mapper.setConfig(mapper.getDeserializationConfig().withView(Views.Normal.class));


        //SimpleModule module = new SimpleModule();
        //module.setDeserializerModifier(new AfterUnmarshalModifier());
        //mapper.registerModule(module);

        try {
            Class<?> avro = Class.forName("nl.vpro.jackson2.SerializeAvroModule");
            register(mapper, filter, (com.fasterxml.jackson.databind.Module) avro.newInstance());
        } catch (ClassNotFoundException ncdfe) {
            if (! loggedAboutAvro) {
                log.debug("SerializeAvroModule could not be registered because: " + ncdfe.getClass().getName() + " " + ncdfe.getMessage());
            }
            loggedAboutAvro = true;
        } catch (IllegalAccessException | InstantiationException e) {
            log.error(e.getMessage(), e);
            loggedAboutAvro = true;
        }

        try {
            Class<?> guava = Class.forName("nl.vpro.jackson2.GuavaRangeModule");
            register(mapper, filter, (com.fasterxml.jackson.databind.Module) guava.newInstance());
        } catch (ClassNotFoundException ncdfe) {
            log.debug(ncdfe.getMessage());
        } catch (IllegalAccessException | InstantiationException e) {
            log.error(e.getMessage(), e);
        }
    }

    public static void addFilter(String key, PropertyFilter filter) {
        FILTER_PROVIDER.addFilter(key, filter);
        log.info("Installed filter {} -> {}", key, filter);
    }

    private static void register(ObjectMapper mapper, Predicate<Module> predicate, Module module) {
        if (predicate.test(module)) {
            mapper.registerModule(module);
        }
    }

    @Override
    public String toString() {
        return Jackson2Mapper.class.getSimpleName() + " (" + toString + ")";
    }
}
