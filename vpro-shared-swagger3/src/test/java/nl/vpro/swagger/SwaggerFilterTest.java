package nl.vpro.swagger;

import java.io.*;
import java.util.*;

import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;

import org.junit.jupiter.api.Test;
import org.meeuw.json.grep.matching.PathMatcher;

import nl.vpro.test.util.jackson2.Jackson2TestUtil;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SwaggerFilterTest {

    @Test
    public void testFilter() throws IOException {
        String input = """
            {
              "openapi": "3.0.1",
              "externalDocs": {
                "description": "NPO Frontend API WIKI",
                "url": "${baseUrl}"
              },
              "servers": [
                {
                  "url": "${api.basePath}"
                }
              ]
            }""";

        Map<String, String> parameters = Map.of("baseUrl", "https://wiki.publiekeomroep.nl/display/npoapi/");

        SwaggerFilter filter = new SwaggerFilter();

        filter.init(new FilterConfig() {
            @Override
            public String getFilterName() {
                return "";
            }

            @Override
            public ServletContext getServletContext() {
                ServletContext context = mock(ServletContext.class);
                when(context.getInitParameter(anyString())).thenReturn(null);
                return context;
            }

            @Override
            public String getInitParameter(String name) {
                return parameters.get(name);
            }

            @Override
            public Enumeration<String> getInitParameterNames() {
                return Collections.enumeration(parameters.keySet());
            }
        });
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        PathMatcher matcher = filter.getPathMatcher("/v3/api", "rs.poms.omroep.nl");
        try (OutputStream out = filter.transform(outputStream, matcher)) {
            out.write(input.getBytes());
        }
        Jackson2TestUtil.assertThatJson(outputStream.toString()).isSimilarTo("""
            {
              "openapi" : "3.0.1",
              "externalDocs" : {
                "description" : "NPO Frontend API WIKI",
                "url" : "https://wiki.publiekeomroep.nl/display/npoapi/"
              },
              "servers" : [ {
                "url" : "rs.poms.omroep.nl/v3/api"
              } ]
            }""");




    }

}
