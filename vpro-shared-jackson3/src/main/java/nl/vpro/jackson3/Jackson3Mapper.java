/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.jackson3;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.Serial;
import java.lang.reflect.InvocationTargetException;
import java.net.http.HttpResponse;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.slf4j.event.Level;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationIntrospector;
import com.google.common.annotations.Beta;

import nl.vpro.logging.simple.SimpleLogger;
import nl.vpro.util.LoggingInputStream;

import static nl.vpro.logging.simple.Slf4jSimpleLogger.slf4j;

/**
 * TODO: Many static public members that are not unmodifiable (e.g. {@link #INSTANCE}).
 * <p>
 * Please use the static getters (like {@link #getInstance()}, so we could change that.
 *
 * @author Rico
 * @author Michiel Meeuwissen

 */
@Slf4j
public class Jackson3Mapper extends ObjectMapper {

    @Serial
    private static final long serialVersionUID = 8353430660109292010L;

    private static boolean loggedAboutAvro = false;
    private static boolean loggedAboutFallback = false;

    private static final SimpleFilterProvider FILTER_PROVIDER = new SimpleFilterProvider();



    @Deprecated
    public static final Jackson3Mapper INSTANCE = getInstance();
    @Deprecated
    public static final Jackson3Mapper LENIENT = getLenientInstance();
    @Deprecated
    public static final Jackson3Mapper STRICT = getStrictInstance();
    @Deprecated
    public static final Jackson3Mapper PRETTY_STRICT = getPrettyStrictInstance();

    @Deprecated
    public static final Jackson3Mapper PRETTY = getPrettyInstance();
    @Deprecated
    public static final Jackson3Mapper PUBLISHER = getPublisherInstance();
    @Deprecated
    public static final Jackson3Mapper PRETTY_PUBLISHER = getPublisherInstance();
    @Deprecated
    public static final Jackson3Mapper BACKWARDS_PUBLISHER = getBackwardsPublisherInstance();


    private static final ThreadLocal<Jackson3Mapper> THREAD_LOCAL = ThreadLocal.withInitial(Jackson3Mapper::getInstance);


    public static Jackson3Mapper getInstance()  {
        Jackson3Mapper mapper =  new Jackson3Mapper("instance");
        mapper.setConfig(mapper.getSerializationConfig().withView(Views.Forward.class));
        mapper.setConfig(mapper.getDeserializationConfig().withView(Views.Forward.class));
        return mapper;

    }

    public static Jackson3Mapper getLenientInstance() {
        Jackson3Mapper lenient = new Jackson3Mapper("lenient");
        lenient.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);
        lenient.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        lenient.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);
        lenient.enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES);
        lenient.setConfig(lenient.getSerializationConfig().withView(Views.Forward.class));
        lenient.setConfig(lenient.getDeserializationConfig().withView(Views.Forward.class));
        return lenient;
    }

    public static Jackson3Mapper getPrettyInstance() {
        Jackson3Mapper pretty = new Jackson3Mapper("pretty");
        pretty.enable(SerializationFeature.INDENT_OUTPUT);
        return pretty;
    }

    public static Jackson3Mapper getPrettyStrictInstance() {
        Jackson3Mapper pretty_and_strict = new Jackson3Mapper("pretty_strict");
        pretty_and_strict.enable(SerializationFeature.INDENT_OUTPUT);

        pretty_and_strict.setConfig(pretty_and_strict.getSerializationConfig().withView(Views.Forward.class));
        pretty_and_strict.setConfig(pretty_and_strict.getDeserializationConfig().withView(Views.Forward.class));
        pretty_and_strict.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES); // This gives quite a lot of troubles. Though I'd like it to be set, especially because PRETTY is used in tests.

        return pretty_and_strict;
    }


    public static Jackson3Mapper getStrictInstance() {
        Jackson3Mapper strict = new Jackson3Mapper("strict");
        strict.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        strict.setConfig(strict.getSerializationConfig().withView(Views.Forward.class));
        strict.setConfig(strict.getDeserializationConfig().withView(Views.Forward.class));

        return strict;
    }

    public static Jackson3Mapper getPublisherInstance() {
        Jackson3Mapper publisher = new Jackson3Mapper("publisher");
        publisher.setConfig(publisher.getSerializationConfig().withView(Views.ForwardPublisher.class));
        publisher.setConfig(publisher.getDeserializationConfig().withView(Views.Forward.class));

        return publisher;
    }

    public static Jackson3Mapper getPrettyPublisherInstance() {
        Jackson3Mapper prettyPublisher = new Jackson3Mapper("pretty_publisher");
        prettyPublisher.setConfig(prettyPublisher.getSerializationConfig().withView(Views.ForwardPublisher.class));
        prettyPublisher.setConfig(prettyPublisher.getDeserializationConfig().withView(Views.Forward.class));
        prettyPublisher.enable(SerializationFeature.INDENT_OUTPUT);
        return prettyPublisher;
    }

    public static Jackson3Mapper getBackwardsPublisherInstance() {
        Jackson3Mapper backwardsPublisher = new Jackson3Mapper("backwards_publisher");
        backwardsPublisher.setConfig(backwardsPublisher.getSerializationConfig().withView(Views.Publisher.class));
        backwardsPublisher.setConfig(backwardsPublisher.getDeserializationConfig().withView(Views.Normal.class));
        backwardsPublisher.enable(SerializationFeature.INDENT_OUTPUT);
        return backwardsPublisher;
    }

    @Beta
    public static Jackson3Mapper getModelInstance() {
        Jackson3Mapper model = new Jackson3Mapper("model");
        model.setConfig(model.getSerializationConfig().withView(Views.Model.class));
        return model;
    }

    @Beta
    public static Jackson3Mapper getModelAndNormalInstance() {
        Jackson3Mapper modalAndNormal = new Jackson3Mapper("model_and_normal");
        modalAndNormal.setConfig(modalAndNormal.getSerializationConfig().withView(Views.ModelAndNormal.class));
        return modalAndNormal;
    }

    public static Jackson3Mapper getThreadLocal() {
        return THREAD_LOCAL.get();
    }
    public static void setThreadLocal(Jackson3Mapper set) {
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

    private Jackson3Mapper(String toString, Predicate<Module> predicate) {
        configureMapper(this, predicate);
        this.toString = toString;
    }

    private Jackson3Mapper(String toString) {
        configureMapper(this);
        this.toString = toString;
    }


    @SafeVarargs
    public static Jackson3Mapper create(String toString, Predicate<Module> module, Consumer<ObjectMapper>... consumer) {
        Jackson3Mapper result =  new Jackson3Mapper(toString, module);
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
             new JakartaXmlBindAnnotationIntrospector(mapper.getTypeFactory())
         );

        mapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_EMPTY);
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
            log.atLevel(  loggedAboutFallback ? Level.DEBUG : Level.WARN).log( noClassDefFoundError.getMessage() + " temporary falling back. Please upgrade jackson");
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
            register(mapper, filter, (com.fasterxml.jackson.databind.Module) avro.getDeclaredConstructor().newInstance());
        } catch (ClassNotFoundException ncdfe) {
            if (! loggedAboutAvro) {
                log.debug("SerializeAvroModule could not be registered because: " + ncdfe.getClass().getName() + " " + ncdfe.getMessage());
            }
            loggedAboutAvro = true;
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
            log.error(e.getMessage(), e);
            loggedAboutAvro = true;
        }

        try {
            Class<?> guava = Class.forName("nl.vpro.jackson3.GuavaRangeModule");
            register(mapper, filter, (com.fasterxml.jackson.databind.Module) guava.getDeclaredConstructor().newInstance());
        } catch (ClassNotFoundException ncdfe) {
            log.debug(ncdfe.getMessage());
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
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
        return Jackson3Mapper.class.getSimpleName() + " (" + toString + ")";
    }

    /**
     * Returns a {@link HttpResponse.BodyHandler} that reads the body as a value of the given type, using this ObjectMapper.
     * @since 5.11
     */
    public <T> HttpResponse.BodyHandler<T> asBodyHandler(Class<T> type) {
        return asBodyHandler(type, nl.vpro.logging.simple.Level.DEBUG);
    }

    /**
     * Returns a {@link HttpResponse.BodyHandler} that reads the body as a value of the given type, using this ObjectMapper.
     * Note that if logging is enabled at the given level commons-io must be available.
     * @since 5.11
     */
    public <T> HttpResponse.BodyHandler<T> asBodyHandler(Class<T> type, nl.vpro.logging.simple.Level level) {

        return new HttpResponse.BodyHandler<T>() {
            @Override
            public HttpResponse.BodySubscriber<T> apply(HttpResponse.ResponseInfo responseInfo) {
                return HttpResponse.BodySubscribers.mapping(
                    HttpResponse.BodySubscribers.ofInputStream(),
                    body -> {
                        try {
                            SimpleLogger simple = slf4j(log);
                            if (simple.isEnabled(level)) {
                                body = new LoggingInputStream(simple, body, level);
                            }
                            return readValue(body, type);

                        } catch (IOException e) {
                            log.warn(e.getMessage(), e);
                            return null;
                        }
                    });
            }
        };
    }
}


