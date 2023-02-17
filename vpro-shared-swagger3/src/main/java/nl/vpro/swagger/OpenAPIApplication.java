/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.swagger;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import io.swagger.v3.oas.integration.api.OpenApiContext;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;

import nl.vpro.jackson2.Jackson2Mapper;
import nl.vpro.jackson2.Views;
import nl.vpro.util.ThreadPools;


/**
 * Static helper to expose a dummy JAX-RS Application. Swagger needs such an application to expose the API docs for
 * the given endpoints.
 *
 * @author Michiel Meeuwissen
 * @since 3.2
 */
@Slf4j
public abstract class OpenAPIApplication {


    //extends Application {

    @Value("${api.version}")
    String apiVersion;

    @Value("${swagger.maxAge}")
    Duration cacheTTL;

    @Value("${documentation.baseUrl}")
    URI documentationBaseUrl;

    @Value("${documentation.email}")
    String email;

    OpenAPI api;

    public abstract Set<Class<?>> getClasses();


    @Bean
    public OpenAPIConfiguration swaggerConfiguration() {
        final SwaggerConfiguration config = new SwaggerConfiguration();
        config.cacheTTL(cacheTTL.toMillis() / 1000);
        config.prettyPrint(false);
        config.setResourceClasses(
            Stream.concat(
                    Set.of(getClass()).stream(), getClasses().stream())
                .map(Class::getName).collect(Collectors.toSet())
        );
        config.setReadAllResources(false);
        log.info("Created {}", config);
        return  config;
    }


    @Bean()
    @Lazy // lazy, we need servlet Config
    OpenApiContext getOpenApiContext(OpenAPIConfiguration openApiConfiguration) throws OpenApiConfigurationException {
        return new JaxrsOpenApiContextBuilder()
            .openApiConfiguration(openApiConfiguration)
            .ctxId("bla")
            .buildContext(true);

    }

    @Bean
    @Lazy// lazy, we need servlet Config
    public OpenAPI getOpenAPI(Environment environment, OpenApiContext ctx) {
        if (api == null) {
            Jackson2Mapper.configureMapper(Json.mapper());
            Json.mapper()
                .registerModule(new JaxbAnnotationModule())
                .registerModule(new JavaTimeModule());
            Json.mapper().setConfig(Json.mapper().getSerializationConfig().withView(Views.Normal.class));

            api = ctx.read();

            //log.info("Found for openapi {}", api.getPaths());

            boolean pretty = ctx.getOpenApiConfiguration() != null &&
                Boolean.TRUE.equals(ctx.getOpenApiConfiguration().isPrettyPrint());


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
