package nl.vpro.swagger;

import java.io.*;

import org.junit.jupiter.api.Test;
import org.meeuw.json.grep.matching.PathMatcher;

import nl.vpro.test.util.jackson2.Jackson2TestUtil;

public class SwaggerFilterTest {

    @Test
    public void testFilter() throws IOException {
        String input = "{" +
            "  \"openapi\": \"3.0.1\",\n" +
            "  \"externalDocs\": {\n" +
            "    \"description\": \"NPO Frontend API WIKI\",\n" +
            "    \"url\": \"https://wiki.publiekeomroep.nl/display/npoapi/\"\n" +
            "  },\n" +
            "  \"servers\": [\n" +
            "    {\n" +
            "      \"url\": \"${api.basePath}\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";

        SwaggerFilter filter = new SwaggerFilter();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        PathMatcher matcher = filter.getPathMatcher("/v3/api", "rs.poms.omroep.nl");
        try (OutputStream out = filter.transform(outputStream, matcher)) {
            out.write(input.getBytes());
        }
        Jackson2TestUtil.assertThatJson(outputStream.toString()).isSimilarTo("{\n" +
            "  \"openapi\" : \"3.0.1\",\n" +
            "  \"externalDocs\" : {\n" +
            "    \"description\" : \"NPO Frontend API WIKI\",\n" +
            "    \"url\" : \"https://wiki.publiekeomroep.nl/display/npoapi/\"\n" +
            "  },\n" +
            "  \"servers\" : [ {\n" +
            "    \"url\" : \"rs.poms.omroep.nl/v3/api\"\n" +
            "  } ]\n" +
            "}");




    }

}
