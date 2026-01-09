/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.jackson3;

import lombok.AccessLevel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.*;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.introspect.AnnotationIntrospectorPair;
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.ser.PropertyFilter;
import tools.jackson.databind.ser.std.SimpleFilterProvider;
import tools.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationIntrospector;

import java.lang.reflect.InvocationTargetException;
import java.net.http.HttpResponse;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.annotations.Beta;

import nl.vpro.jackson.Views;
import nl.vpro.logging.simple.SimpleLogger;
import nl.vpro.util.LoggingInputStream;

import static nl.vpro.logging.simple.Slf4jSimpleLogger.slf4j;
import static tools.jackson.core.json.JsonReadFeature.*;
import static tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static tools.jackson.databind.MapperFeature.DEFAULT_VIEW_INCLUSION;
import static tools.jackson.databind.MapperFeature.USE_WRAPPER_NAME_AS_PROPERTY_NAME;
import static tools.jackson.databind.SerializationFeature.INDENT_OUTPUT;

/**
 * A wrapper around a Jackson {@link ObjectMapper} with one {@link ObjectReader} and one {@link ObjectWriter} configured with specific views.
 * <p>
 * In the jackson2 version this was itself an ObjectMapper which could have been configured with default views and so on.
 * In jackson3 you can actually define multiple sets of configuration in the mapper itself (with serializationContexts) , but that is a bit more cumbersome to use, and deviates
 * more from the original idea.
 *
 * @author Rico
 * @author Michiel Meeuwissen
 * @since 5.14
 */
@Slf4j

public class Jackson3Mapper {

    private static boolean loggedAboutAvro = false;

    private static final SimpleFilterProvider FILTER_PROVIDER = new SimpleFilterProvider();

    public static final Jackson3Mapper INSTANCE = Jackson3Mapper.builder("instance").build();
    public static final Jackson3Mapper LENIENT = buildLenientInstance();
    public static final Jackson3Mapper STRICT = buildStrictInstance();
    public static final Jackson3Mapper PRETTY_STRICT = buildPrettyStrictInstance();
    public static final Jackson3Mapper PRETTY = buildPrettyInstance();
    public static final Jackson3Mapper PUBLISHER = buildPublisherInstance();
    public static final Jackson3Mapper PRETTY_PUBLISHER = buildPublisherInstance();
    public static final Jackson3Mapper BACKWARDS_PUBLISHER = buildBackwardsPublisherInstance();

    public static final Jackson3Mapper MODEL = buildModelInstance();
    public static final Jackson3Mapper MODEL_AND_NORMAL = buildModelAndNormalInstance();


    public static Jackson3Mapper getInstance() {
        return INSTANCE;
    }
    public static Jackson3Mapper getLenientInstance() {
        return LENIENT;
    }
    public static Jackson3Mapper getPrettyInstance() {
        return PRETTY;
    }

    public static Jackson3Mapper getPrettyStrictInstance() {
        return PRETTY_STRICT;
    }

    public static Jackson3Mapper getStrictInstance() {
        return STRICT;
    }

    public static Jackson3Mapper getPublisherInstance() {
        return PUBLISHER;
    }

    public static Jackson3Mapper getPrettyPublisherInstance() {
        return PRETTY_PUBLISHER;
    }

    public static Jackson3Mapper getBackwardsPublisherInstance() {
        return BACKWARDS_PUBLISHER;
    }

    @Beta
    public static Jackson3Mapper getModelInstance() {
        return MODEL;
    }

    @Beta
    public static Jackson3Mapper getModelAndNormalInstance() {
        return MODEL_AND_NORMAL;
    }



    private static final ThreadLocal<Jackson3Mapper> THREAD_LOCAL = ThreadLocal.withInitial(() -> INSTANCE);


    private static Jackson3Mapper buildLenientInstance() {
        return Jackson3Mapper.builder("lenient")
            .forward()
            .configure(Jackson3Mapper::lenient)
            .build();
    }

    private static void lenient(JsonMapper.Builder builder)  {
        builder.enable(EnumFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);
        builder.disable(FAIL_ON_UNKNOWN_PROPERTIES);
        builder.enable(ALLOW_UNQUOTED_PROPERTY_NAMES);
        builder.enable(ALLOW_SINGLE_QUOTES);
    }
    private static void strict(JsonMapper.Builder builder)  {
        builder.enable(FAIL_ON_UNKNOWN_PROPERTIES);
    }

    private static Jackson3Mapper buildPrettyInstance() {
        Jackson3Mapper.Builder pretty = Jackson3Mapper
            .builder("pretty")
            .configure(b -> {
                lenient(b);
                b.enable(INDENT_OUTPUT);
            });
        return pretty.build();
    }

    private static Jackson3Mapper buildPrettyStrictInstance() {

        return Jackson3Mapper
            .builder("pretty_strict")
            .configure(b -> {
                strict(b);
                b.enable(INDENT_OUTPUT);
                b.enable(FAIL_ON_UNKNOWN_PROPERTIES);
            })
            .forward()
            .build()
            ;
    }


    private static Jackson3Mapper buildStrictInstance() {
        return Jackson3Mapper.builder("strict")
            .configure(b -> {
                b.enable(FAIL_ON_UNKNOWN_PROPERTIES);
                }
            ).forward()
            .build();
    }

    private static Jackson3Mapper buildPublisherInstance() {
        return Jackson3Mapper.builder("publisher")
            .serializationView(Views.ForwardPublisher.class)
            .deserializationView(Views.Forward.class)
            .build();
    }

    private static Jackson3Mapper buildPrettyPublisherInstance() {
        return Jackson3Mapper.builder("pretty_publisher")
            .serializationView(Views.ForwardPublisher.class)
            .deserializationView(Views.Forward.class)
            .configure(b -> b.enable(INDENT_OUTPUT))
            .build();
    }

    private static Jackson3Mapper buildBackwardsPublisherInstance() {
        return Jackson3Mapper.builder("backwards_publisher")
            .serializationView(Views.Publisher.class)
            .deserializationView(Views.Normal.class)
            .configure(b -> b.enable(INDENT_OUTPUT))
            .build();
    }

    private static Jackson3Mapper buildModelInstance() {
        return Jackson3Mapper.builder("model")
            .serializationView(Views.Model.class)
            .build();
    }

    private static Jackson3Mapper buildModelAndNormalInstance() {
        return Jackson3Mapper.builder("model_and_normal")
            .serializationView(Views.ModelAndNormal.class)
            .build();

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

    @SneakyThrows({JacksonException.class})
    public static <T> T lenientTreeToValue(JsonNode jsonNode, Class<T> clazz) {
        return buildLenientInstance().reader().treeToValue(jsonNode, clazz);
    }

    private final JsonMapper mapper;
    private final ObjectWriter writer;
    private final ObjectReader reader;
    private final String toString;

    @lombok.Builder(
        builderMethodName = "_builder",
        buildMethodName = "_build",
        access = AccessLevel.PRIVATE)
    private Jackson3Mapper(
        String toString,
        JsonMapper mapper,
        Class<?> serializationView,
        Class<?> deserializationView) {
        this.mapper = mapper;
        this.writer = mapper.writerWithView(serializationView == null ? Views.Normal.class : serializationView);
        this.reader =  mapper.readerWithView(deserializationView == null ? Views.Normal.class : deserializationView);
        this.toString = toString;
    }

    public static void configureMapper(Jackson3Mapper.Builder mapper) {
        configureMapper(mapper, m -> true);
    }

    public static void configureMapper(Jackson3Mapper.Builder builder, Predicate<JacksonModule> filter) {

        builder.mapperBuilder.filterProvider(FILTER_PROVIDER);

        AnnotationIntrospector introspector = new AnnotationIntrospectorPair(
            new JacksonAnnotationIntrospector(),
            new JakartaXmlBindAnnotationIntrospector(false)
        );

        builder.mapperBuilder.changeDefaultPropertyInclusion(v -> v.withContentInclusion(JsonInclude.Include.NON_EMPTY));
        builder.mapperBuilder.annotationIntrospector(introspector);

        builder.mapperBuilder.disable(FAIL_ON_UNKNOWN_PROPERTIES); // This seems a good idea when reading from couchdb or so, but when reading user supplied forms, it is confusing not getting errors.

        builder.mapperBuilder.enable(ALLOW_SINGLE_QUOTES);
        builder.mapperBuilder.enable(ALLOW_UNQUOTED_PROPERTY_NAMES);
        builder.mapperBuilder.enable(USE_WRAPPER_NAME_AS_PROPERTY_NAME);
        builder.mapperBuilder.enable(ALLOW_JAVA_COMMENTS);
        builder.mapperBuilder.enable(ALLOW_LEADING_ZEROS_FOR_NUMBERS);


        // actually a breaking change in jackson3, this defaults to false now.
        builder.mapperBuilder.enable(DEFAULT_VIEW_INCLUSION);

        register(builder, filter, new DateModule());


        //SimpleModule module = new SimpleModule();
        //module.setDeserializerModifier(new AfterUnmarshalModifier());
        //mapper.registerModule(module);

        try {
            Class<?> avro = Class.forName("nl.vpro.jackson3.SerializeAvroModule");
            register(builder, filter, (tools.jackson.databind.JacksonModule) avro.getDeclaredConstructor().newInstance());
        } catch (ClassNotFoundException ncdfe) {
            if (! loggedAboutAvro) {
                log.debug("SerializeAvroModule could not be registered because: {} {}", ncdfe.getClass().getName(), ncdfe.getMessage());
            }
            loggedAboutAvro = true;
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
            log.error(e.getMessage(), e);
            loggedAboutAvro = true;
        }

        try {
            Class<?> guava = Class.forName("nl.vpro.jackson3.GuavaRangeModule");
            register(builder, filter, (tools.jackson.databind.JacksonModule) guava.getDeclaredConstructor().newInstance());
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

    private static void register(Jackson3Mapper.Builder mapper, Predicate<JacksonModule> predicate, JacksonModule module) {
        if (predicate.test(module)) {
            mapper.mapperBuilder.addModule(module);
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
                        SimpleLogger simple = slf4j(log);
                        if (simple.isEnabled(level)) {
                            body = new LoggingInputStream(simple, body, level);
                        }
                        return mapper.readValue(body, type);

                    });
            }
        };
    }

    public JsonMapper mapper() {
        return mapper;
    }

    public ObjectWriter writer() {
        return writer;
    }

    public ObjectReader reader() {
        return reader;
    }
    public ObjectReader readerFor(Class<?> clazz) {
        return reader.forType(clazz);
    }

    public Builder rebuild() {
        Builder builder =  builder(toString);
        builder.serializationView(writer.getConfig().getActiveView());
        builder.deserializationView(reader.getConfig().getActiveView());
        builder.mapperBuilder = mapper.rebuild();
        return builder;
    }

    public static Builder builder(String toString) {
        return _builder()
            .toString(toString);
    }

    public static class Builder {
        private JsonMapper.Builder mapperBuilder = JsonMapper
            .builder();
        {
            configureMapper(this);
        }




        public Jackson3Mapper.Builder forward() {
            return serializationView(Views.Forward.class)
                .deserializationView(Views.Forward.class);
        }


        public Jackson3Mapper build() {
            mapper(mapperBuilder.build());
            return _build();
        }

        public Builder configure(Consumer<JsonMapper.Builder> consumer) {
            consumer.accept(mapperBuilder);
            return this;
        }


    }
}

