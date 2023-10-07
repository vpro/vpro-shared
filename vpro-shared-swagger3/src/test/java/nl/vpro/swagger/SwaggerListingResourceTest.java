package nl.vpro.swagger;

import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import io.swagger.v3.oas.integration.api.OpenApiContext;
import io.swagger.v3.oas.models.OpenAPI;

import java.util.Arrays;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SwaggerListingResourceTest {

    SwaggerListingResource impl = new SwaggerListingResource(() -> mock(OpenAPIConfiguration.class)
        , () -> mock(OpenApiContext.class)
        , () -> mock(OpenAPI.class)

    );

    @Test
    void isYaml() {
        HttpHeaders headers = mock(HttpHeaders.class);
        when(headers.getAcceptableMediaTypes()).thenReturn(Arrays.asList(new MediaType("application", "yaml")));
        assertThat(SwaggerListingResource.isYaml(headers)).isTrue();

        when(headers.getAcceptableMediaTypes()).thenReturn(Arrays.asList(new MediaType("application", "yaml", "UTF-8")));
        assertThat(SwaggerListingResource.isYaml(headers)).isTrue();

        when(headers.getAcceptableMediaTypes()).thenReturn(Arrays.asList(new MediaType("*", "*")));
        assertThat(SwaggerListingResource.isYaml(headers)).isFalse();

        when(headers.getAcceptableMediaTypes()).thenReturn(Arrays.asList(new MediaType("application", "*")));
        assertThat(SwaggerListingResource.isYaml(headers)).isFalse();

        when(headers.getAcceptableMediaTypes()).thenReturn(Arrays.asList(new MediaType("application", "json")));
        assertThat(SwaggerListingResource.isYaml(headers)).isFalse();

        when(headers.getAcceptableMediaTypes()).thenReturn(Arrays.asList(new MediaType("application", "json", "UTF-8")));
        assertThat(SwaggerListingResource.isYaml(headers)).isFalse();

    }
}
