package nl.vpro.swagger;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import org.junit.Test;

import nl.vpro.jackson2.JsonFilter;

public class SwaggerFilterTest {

    @Test
    public void testFilter() throws IOException, ExecutionException, InterruptedException {
        String input = "{apiVersion: \"3.0\",\n" +
            "swaggerVersion: \"1.2\",\n" +
            "basePath: \"${api.basePath}\"}";

        JsonFilter.Replacement<String> replacement = new JsonFilter.Replacement<>("basePath", "${api.basePath}", "bla");
        SwaggerFilter filter = new SwaggerFilter();
        OutputStream out = filter.transform(System.out, Arrays.asList(replacement));
        out.write(input.getBytes());
        out.close();




    }

}
