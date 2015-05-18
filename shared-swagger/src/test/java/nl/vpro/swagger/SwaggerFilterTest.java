package nl.vpro.swagger;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import org.junit.Test;

public class SwaggerFilterTest {

    @Test
    public void testFilter() throws IOException, ExecutionException, InterruptedException {
        String input = "{apiVersion: \"3.0\",\n" +
            "swaggerVersion: \"1.2\",\n" +
            "basePath: \"${api.basePath}\"}";

        SwaggerFilter.Replacement <String> replacement = new SwaggerFilter.Replacement<String>();
        replacement.key = "basePath";
        replacement.value = "${api.basePath}";
        replacement.newValue = "bla";

        SwaggerFilter filter = new SwaggerFilter();
        OutputStream out = filter.transform(System.out, Arrays.asList(replacement));

        out.write(input.getBytes());

        out.close();

    }

}
