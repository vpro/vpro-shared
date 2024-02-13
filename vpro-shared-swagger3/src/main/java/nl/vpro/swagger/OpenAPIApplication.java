/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.swagger;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import io.swagger.v3.oas.integration.api.OpenApiContext;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;

import java.net.URI;
import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.PreDestroy;


import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationModule;

import nl.vpro.jackson2.Jackson2Mapper;
import nl.vpro.jackson2.Views;
import nl.vpro.rs.ResteasyApplication;
import nl.vpro.swagger.model.*;
import nl.vpro.util.ThreadPools;


/**
 * Static helper to expose a JAX-RS Application to API. Swagger needs such an application to expose the API docs for
 * the given endpoints.
 *
 * @author Michiel Meeuwissen
 * @since 3.2
 */
@Slf4j
public abstract class OpenAPIApplication {


    //extends Application {

    @Value("${openapi.info.version}")
    String apiVersion;

    @Value("${swagger.maxAge:PT1H}")
    Duration cacheTTL;

    @Value("${documentation.baseUrl}")
    URI documentationBaseUrl;

    @Value("${documentation.email}")
    String email;

    OpenAPI api;

    protected Set<Class<?>> getClasses() {
        Set<Class<?>> result =
            Stream.concat(
                    ResteasyApplication.getInstance()
                        .getSingletons().stream()
                        .map(Object::getClass),
                    ResteasyApplication.getInstance()
                        .getClasses().stream()
                )
                .filter(c ->
                    c.getAnnotation(OpenAPIDefinition.class) != null
                )
            .collect(Collectors.toSet());
        log.info("Using {} for openapi", result);
        return result;
    }


    @Bean
    @Lazy
    public OpenAPIConfiguration swaggerConfiguration() {
        ModelConverters.getInstance().addConverter(new EnumModelConverter());
        ModelConverters.getInstance().addConverter(new LocaleModelConverter());
        ModelConverters.getInstance().addConverter(new DurationModelConverter());
        final SwaggerConfiguration config = new SwaggerConfiguration();
        config.cacheTTL(cacheTTL.toMillis() / 1000);
        config.prettyPrint(false);
        Set<String> resourceClasses =
            Stream.concat(
                Set.of(getClass()).stream(),
                getClasses().stream()
                ).map(Class::getName).collect(Collectors.toSet());
        log.info("Configured open api with resources classes {}", resourceClasses);
        config.setResourceClasses(resourceClasses);
        config.setReadAllResources(false);
        log.info("Created {}", config);
        return  config;
    }


    @Bean()
    @Lazy
    OpenApiContext getOpenApiContext(OpenAPIConfiguration openApiConfiguration) throws OpenApiConfigurationException {
        return new JaxrsOpenApiContextBuilder<>()
            .openApiConfiguration(openApiConfiguration)
            .ctxId("our-id")
            .buildContext(true);

    }

    @Bean
    @Lazy
    public OpenAPI getOpenAPI(Environment environment, OpenApiContext ctx) {
        if (api == null) {
            Jackson2Mapper.configureMapper(Json.mapper());
            Json.mapper()
                .registerModule(new JakartaXmlBindAnnotationModule())
                .registerModule(new JavaTimeModule());

            Json.mapper().setConfig(
                Json.mapper().getSerializationConfig().withView(Views.Normal.class));

            api = ctx.read();


            Info info = api.getInfo();
            if (info == null) {
                info = new Info();
                api.info(info);
            }
            info.setVersion(apiVersion);
            Contact contact = new Contact();
            contact.setUrl(documentationBaseUrl.toString());
            contact.setEmail(email);
            info.setContact(contact);
            fixDocumentation(api);
            log.info("Assembled {}", info);
        } else {
            log.info("Returning previously assembled {}", api.getInfo());
        }

        return api;
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down {}", this);
        ThreadPools.shutdown();
    }

    protected void fixDocumentation(OpenAPI api) {
        if (api.getTags() != null) {
            api.getTags().forEach(t -> {
                fixDocumentation(t.getExternalDocs());
            });
        }
    }

    @SneakyThrows
    protected void fixDocumentation(io.swagger.v3.oas.models.ExternalDocumentation documentation) {
        if (documentation != null) {
            URI url = URI.create(documentation.getUrl());
            URI newUri = new URI(
                documentationBaseUrl.getScheme(),
                documentationBaseUrl.getAuthority(),
                url.getPath(),
                url.getQuery(),
                url.getFragment());
            documentation.setUrl(newUri.toString());
        }
    }


}
