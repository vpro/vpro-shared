/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.rs;

import lombok.extern.slf4j.Slf4j;

import java.util.*;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;


/**
 * Static helper to expose a dummy JAX-RS Application. Swagger needs such an application to expose the API docs for
 * the given endpoints.
 * <p>
 * Note, this used to be SwaggerApplication in vpro-shared-swagger. But it's not related to swagger. It has to do with RestEasy.
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
@Slf4j
@ApplicationPath("")
public class ResteasyApplication extends Application {
    private static final Set<Object> singletons = new HashSet<>();


    private static ResteasyApplication INSTANCE;

    public ResteasyApplication() {
        INSTANCE = this;
    }

    public static ResteasyApplication getInstance() {
        return INSTANCE;
    }

    @Override
    public Set<Class<?>> getClasses() {
        return Set.of();
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }
    /**
     * Inject Swagger annotated JAX-RS ap's you want to publish. Swagger only calls #getSingletons on the first
     * API documentation request, so you must take care that your services get injected before an application accepts
     * web requests. (Using @PostConstruct on your services is a viable solution)
     *
     * @param services
     */
    public static void inject(Object... services) {
        List<Object> list = Arrays.asList(services);
        log.info("Injecting singletons {}", list);
        singletons.addAll(list);
    }



}
