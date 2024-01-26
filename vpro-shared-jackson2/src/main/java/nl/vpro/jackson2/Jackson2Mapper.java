/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.jackson2;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.Serial;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.slf4j.event.Level;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.Module;
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
 * TODO: Many static public members that are not unmodifiable (e.g. {@link #INSTANCE}).
 * <p>
 * Please use the static getters (like {@link #getInstance()}, so we could change that.
 *
 * @author Rico
 * @author Michiel Meeuwissen

 */
@Slf4j
public class Jackson2Mapper extends ObjectMapper {

    @Serial
    private static final long serialVersionUID = 8353430660109292010L;

    private static boolean loggedAboutAvro = false;
    private static boolean loggedAboutFallback = false;

    private static final SimpleFilterProvider FILTER_PROVIDER = new SimpleFilterProvider();



    @Deprecated
    public static final Jackson2Mapper INSTANCE = getInstance();
    @Deprecated
    public static final Jackson2Mapper LENIENT = getLenientInstance();
    @Deprecated
    public static final Jackson2Mapper STRICT = getStrictInstance();
    @Deprecated
    public static final Jackson2Mapper PRETTY_STRICT = getPrettyStrictInstance();

    @Deprecated
    public static final Jackson2Mapper PRETTY = getPrettyInstance();
    @Deprecated
    public static final Jackson2Mapper PUBLISHER = getPublisherInstance();
    @Deprecated
    public static final Jackson2Mapper PRETTY_PUBLISHER = getPublisherInstance();
    @Deprecated
    public static final Jackson2Mapper BACKWARDS_PUBLISHER = getBackwardsPublisherInstance();


    private static final ThreadLocal<Jackson2Mapper> THREAD_LOCAL = ThreadLocal.withInitial(Jackson2Mapper::getInstance);


    public static Jackson2Mapper getInstance()  {
        Jackson2Mapper mapper =  new Jackson2Mapper("instance");
        mapper.setConfig(mapper.getSerializationConfig().withView(Views.Forward.class));
        mapper.setConfig(mapper.getDeserializationConfig().withView(Views.Forward.class));
        return mapper;

    }

    public static Jackson2Mapper getLenientInstance() {
        Jackson2Mapper lenient = new Jackson2Mapper("lenient");
        lenient.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);
        lenient.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        lenient.setConfig(lenient.getSerializationConfig().withView(Views.Forward.class));
        lenient.setConfig(lenient.getDeserializationConfig().withView(Views.Forward.class));
        return lenient;
    }

    public static Jackson2Mapper getPrettyInstance() {
        Jackson2Mapper pretty = new Jackson2Mapper("pretty");
        pretty.enable(SerializationFeature.INDENT_OUTPUT);
        return pretty;
    }

    public static Jackson2Mapper getPrettyStrictInstance() {
        Jackson2Mapper pretty_and_strict = new Jackson2Mapper("pretty_strict");
        pretty_and_strict.enable(SerializationFeature.INDENT_OUTPUT);

        pretty_and_strict.setConfig(pretty_and_strict.getSerializationConfig().withView(Views.Forward.class));
        pretty_and_strict.setConfig(pretty_and_strict.getDeserializationConfig().withView(Views.Forward.class));
        pretty_and_strict.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES); // This gives quite a lot of troubles. Though I'd like it to be set, especially because PRETTY is used in tests.

        return pretty_and_strict;
    }


    public static Jackson2Mapper getStrictInstance() {
        Jackson2Mapper strict = new Jackson2Mapper("strict");
        strict.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        strict.setConfig(strict.getSerializationConfig().withView(Views.Forward.class));
        strict.setConfig(strict.getDeserializationConfig().withView(Views.Forward.class));

        return strict;
    }

    public static Jackson2Mapper getPublisherInstance() {
        Jackson2Mapper publisher = new Jackson2Mapper("publisher");
        publisher.setConfig(publisher.getSerializationConfig().withView(Views.ForwardPublisher.class));
        publisher.setConfig(publisher.getDeserializationConfig().withView(Views.Forward.class));

        return publisher;
    }

    public static Jackson2Mapper getPrettyPublisherInstance() {
        Jackson2Mapper prettyPublisher = new Jackson2Mapper("pretty_publisher");
        prettyPublisher.setConfig(prettyPublisher.getSerializationConfig().withView(Views.ForwardPublisher.class));
        prettyPublisher.setConfig(prettyPublisher.getDeserializationConfig().withView(Views.Forward.class));
        prettyPublisher.enable(SerializationFeature.INDENT_OUTPUT);
        return prettyPublisher;
    }

    public static Jackson2Mapper getBackwardsPublisherInstance() {
        Jackson2Mapper backwardsPublisher = new Jackson2Mapper("backwards_publisher");
        backwardsPublisher.setConfig(backwardsPublisher.getSerializationConfig().withView(Views.Publisher.class));
        backwardsPublisher.setConfig(backwardsPublisher.getDeserializationConfig().withView(Views.Normal.class));
        backwardsPublisher.enable(SerializationFeature.INDENT_OUTPUT);
        return backwardsPublisher;
    }

    @Beta
    public static Jackson2Mapper getModelInstance() {
        Jackson2Mapper model = new Jackson2Mapper("model");
        model.setConfig(model.getSerializationConfig().withView(Views.Model.class));
        return model;
    }

    @Beta
    public static Jackson2Mapper getModelAndNormalInstance() {
        Jackson2Mapper modalAndNormal = new Jackson2Mapper("model_and_normal");
        modalAndNormal.setConfig(modalAndNormal.getSerializationConfig().withView(Views.ModelAndNormal.class));
        return modalAndNormal;
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

    private Jackson2Mapper(String toString, Predicate<Module> predicate) {
        configureMapper(this, predicate);
        this.toString = toString;
    }

    private Jackson2Mapper(String toString) {
        configureMapper(this);
        this.toString = toString;
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
        // jdk8Module.configureAbsentsAsNulls(true); This I think it covered by com.fasterxml.jackson.annotation.JsonInclude.Include.NON_ABSENT
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
