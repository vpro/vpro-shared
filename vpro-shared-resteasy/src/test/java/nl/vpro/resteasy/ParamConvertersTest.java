package nl.vpro.resteasy;

import lombok.extern.slf4j.Slf4j;
import ru.lanwen.wiremock.ext.WiremockResolver;
import ru.lanwen.wiremock.ext.WiremockUriResolver;

import java.time.*;
import java.util.Date;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClientEngine;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.github.tomakehurst.wiremock.WireMockServer;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
@Slf4j
@ExtendWith({
    WiremockResolver.class,
    WiremockUriResolver.class
})
public class ParamConvertersTest {

    @Path("/api")

    public interface TestRestService {

        @Path("/a")
        @GET
        @Produces({MediaType.TEXT_PLAIN})
        String a(@QueryParam("instant") Instant instant);


        @Path("/b/{date}")
        @GET
        @Produces({MediaType.TEXT_PLAIN})
        @DateFormat("yyyy-MM-dd")
        String b(@PathParam("date")@DateFormat("yyyy-MM-dd")  Date instant); // Resteasy geeft deze annotaties helemaal niet door?

        @Path("/c")
        @GET
        @Produces({MediaType.TEXT_PLAIN})
        String c();

    }

    @Test
    public void testClientA(
        @WiremockResolver.Wiremock WireMockServer server,
        @WiremockUriResolver.WiremockUri String uri) {

        server.stubFor(
            get(urlEqualTo("/api/a?instant=2020-02-17T19%3A00%3A00Z"))
                .willReturn(okForContentType("text/plain", "foo bar a"))
        );

        TestRestService simple = service(uri);
        Instant instant = LocalDateTime.of(2020, 2, 17, 20, 0).atZone(ZoneId.of("Europe/Amsterdam")).toInstant();
        String s = simple.a(instant);
        assertThat(s).isEqualTo("foo bar a");


    }

    @Test
    //@Disabled("I suppose i don't understand something")
    public void testClientB(
        @WiremockResolver.Wiremock WireMockServer server,
        @WiremockUriResolver.WiremockUri String uri) {

        server.stubFor(
            get(urlEqualTo("/api/b/2020-02-17"))
                .willReturn(okForContentType("text/plain", "foo bar b"))
        );


        TestRestService simple = service(uri);
        log.info("{}", simple);
        Instant instant = LocalDateTime.of(2020, 2, 17, 20, 0).atZone(ZoneId.of("Europe/Amsterdam")).toInstant();
        simple.b(Date.from(instant));




    }


    private TestRestService service(String uri) {
        ResteasyClientBuilder builder = new ResteasyClientBuilderImpl()
             .httpEngine(ApacheHttpClientEngine.create())
             ;
        //builder.register(new DateParamConverterProvider());
        //builder.register(DateFormatter.class);



        ResteasyWebTarget target = builder.build()
            .target(uri);

        TestRestService simple = target
            .proxyBuilder(TestRestService.class)
            .defaultConsumes(MediaType.TEXT_PLAIN_TYPE)
            .defaultProduces(MediaType.TEXT_PLAIN_TYPE)
            .build();

        log.info("{}", simple);
        return simple;
    }
}
