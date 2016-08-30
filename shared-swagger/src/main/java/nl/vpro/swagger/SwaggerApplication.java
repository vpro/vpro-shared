/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.swagger;

import io.swagger.annotations.ApiModel;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;


/**
 * Static helper to expose a dummy JAX-RS Application. Swagger needs such an application to expose the API docs for
 * the given endpoints.
 *
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
@ApiModel
public class SwaggerApplication extends Application {
    private static final Set<Object> singletons = new HashSet<>();

    {
      /*  this.addDeserializer(Parameter.class, new InstantParamProcessor());

        ParameterDeserializer.getInstance().addConverter(new InstantParamProcessor());
*/
    }
    

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> set = new HashSet<>();
        for(Object singleton : singletons) {
            set.add(singleton.getClass());
        }
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
}
