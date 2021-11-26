package nl.vpro.swagger;

import java.io.*;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import nl.vpro.jackson2.JsonFilter;
import nl.vpro.test.util.jackson2.Jackson2TestUtil;

public class SwaggerFilterTest {

    @Test
    public void testFilter() throws IOException {
        String input = "{apiVersion: \"3.0\",\n" +
            "swaggerVersion: \"1.2\",\n" +
            "basePath: \"${api.basePath}\"}";

        JsonFilter.Replacement<String> replacement = new JsonFilter.Replacement<>("basePath", "${api.basePath}", "bla");
        SwaggerFilter filter = new SwaggerFilter();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        OutputStream out = filter.transform(outputStream, Arrays.asList(replacement));
        out.write(input.getBytes());
        out.close();
        Jackson2TestUtil.assertThatJson(outputStream.toString()).isSimilarTo("{\n" +
            "  \"apiVersion\" : \"3.0\",\n" +
            "  \"swaggerVersion\" : \"1.2\",\n" +
            "  \"basePath\" : \"bla\"\n" +
            "}");




    }

}
