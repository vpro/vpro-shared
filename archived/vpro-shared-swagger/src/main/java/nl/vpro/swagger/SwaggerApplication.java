/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.swagger;

import io.swagger.util.PrimitiveType;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.core.Application;

import com.google.common.collect.ImmutableMap;


/**
 * Static helper to expose a dummy JAX-RS Application. Swagger needs such an application to expose the API docs for
 * the given endpoints.
 *
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
@Slf4j
public class SwaggerApplication extends Application {
    private static final Set<Object> singletons = new HashSet<>();
    static {
        //XTrustProvider.install();
        // swagger sucks a lot.
        //https://github.com/swagger-api/swagger-core/issues/1444
        setExternalTypes(ImmutableMap.<Class, PrimitiveType>builder()
            .put(Duration.class, PrimitiveType.STRING)
            .put(Instant.class, PrimitiveType.DATE_TIME)
            .put(LocalDateTime.class, PrimitiveType.DATE_TIME)
            .put(LocalDate.class, PrimitiveType.DATE)
            .build());
    }


    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> set = new HashSet<>();
        return set;
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }

    /**
     * Inject Swagger annotated JAX-RS api's you want to publish. Swagger only calls #getSingletons on the first
     * API documentation request, so you must take care that your services get injected before an application accepts
     * web requests. (Using @PostConstruct on your services is a viable solution)
     *
     * @param services
     */
    public static void inject(Object... services) {
        singletons.addAll(Arrays.asList(services));
    }

    private static void setExternalTypes(Map<Class, PrimitiveType> externalTypes) {
        // ugly hack until swagger supports adding external classes as primitive types
        try {
            Field externalTypesField = PrimitiveType.class.getDeclaredField("EXTERNAL_CLASSES");
            Field modifiersField = Field.class.getDeclaredField("modifiers");

            modifiersField.setAccessible(true);
            externalTypesField.setAccessible(true);
            modifiersField.set(externalTypesField, externalTypesField.getModifiers() & ~Modifier.FINAL);

            Map<String, PrimitiveType> externalTypesInternal = externalTypes.entrySet().stream()
                    .collect(Collectors.toMap(e -> e.getKey().getName(), e -> e.getValue()));
            externalTypesField.set(null, externalTypesInternal);
        } catch (NoSuchFieldException|IllegalAccessException e) {
            log.warn("Couldn't set external types", e);
        }
    }

}
