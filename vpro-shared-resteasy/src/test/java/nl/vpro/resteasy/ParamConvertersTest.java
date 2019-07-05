package nl.vpro.resteasy;

import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClientEngine;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.junit.Test;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
@Slf4j
public class ParamConvertersTest {


    public interface TestRestService {

        @Path("/a")
        @GET
        @Produces({MediaType.TEXT_PLAIN})
        String a(@QueryParam("instant") Instant instant);


        @Path("/b")
        @GET
        @Produces({MediaType.TEXT_PLAIN})
        @DateFormat("yyyy-MM-dd")
        String b(@DateFormat("yyyy-MM-dd")  @QueryParam("instant") Instant instant); // Resteasy geeft deze annotaties helemaal niet door?

        @Path("/c")
        @GET
        @Produces({MediaType.TEXT_PLAIN})
        String c();

    }

    @Test(expected = NotFoundException.class)
    public void testClient() {

         ResteasyClientBuilder builder = new ResteasyClientBuilderImpl()
             .httpEngine(ApacheHttpClientEngine.create())

             ;
        builder.register(new DateParamConverterProvider());

        ResteasyWebTarget target = builder.build()
            .target("http://example.com/base/uri");
        TestRestService simple = target
            .proxyBuilder(TestRestService.class)
            .defaultConsumes(MediaType.TEXT_PLAIN_TYPE)
            .defaultProduces(MediaType.TEXT_PLAIN_TYPE)
            .build();

        log.info("{}", simple);
        simple.b(Instant.now());



    }


}
