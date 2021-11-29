package nl.vpro.swagger;

import java.io.*;

import org.junit.jupiter.api.Test;
import org.meeuw.json.grep.matching.PathMatcher;

import nl.vpro.test.util.jackson2.Jackson2TestUtil;

public class SwaggerFilterTest {

    @Test
    public void testFilter() throws IOException {
        String input = "{apiVersion: \"3.0\",\n" +
            "swaggerVersion: \"1.2\",\n" +
            "basePath: \"${api.basePath}\"}";

        SwaggerFilter filter = new SwaggerFilter();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        PathMatcher matcher = filter.getPathMatcher("/v3/api", "rs.poms.omroep.nl:443");
        try (OutputStream out = filter.transform(outputStream, matcher)) {
            out.write(input.getBytes());
        }
        Jackson2TestUtil.assertThatJson(outputStream.toString()).isSimilarTo("{\n" +
            "  \"apiVersion\" : \"3.0\",\n" +
            "  \"swaggerVersion\" : \"1.2\",\n" +
            "  \"basePath\" : \"/v3/api\n" +
            "}");




    }

}
