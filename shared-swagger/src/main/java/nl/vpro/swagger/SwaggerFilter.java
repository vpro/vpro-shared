package nl.vpro.swagger;

import java.io.*;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.*;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * @author Michiel Meeuwissen
 * @since 0.21
 */
public class SwaggerFilter implements Filter {

    private static ExecutorService executor = Executors.newCachedThreadPool();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        Replacement<String> replacement = new Replacement<String>();
        replacement.key = "basePath";
        replacement.value = "${api.basePath}";
        HttpServletRequest req = (HttpServletRequest) request;
        replacement.newValue = req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort() + req.getContextPath() + "/api";
        List<Replacement> replacements = Arrays.asList(replacement);
        final OutputStream out = transform(response.getOutputStream(), replacements);
        HttpServletResponseWrapper wrapped = new HttpServletResponseWrapper((HttpServletResponse) response) {
            @Override
            public ServletOutputStream getOutputStream() {
                return new ServletOutputStream() {
                    @Override
                    public void write(int b) throws IOException {
                        out.write(b);

                    }

                    @Override
                    public void write(byte[] b) throws IOException {
                        out.write(b);
                    }

                    @Override
                    public void write(byte[] b, int off, int len) throws IOException {
                        out.write(b, off, len);
                    }
                };
            }
        };

        chain.doFilter(request, wrapped);
        out.close();

    }

    @Override
    public void destroy() {



    }

    public OutputStream transform(OutputStream from, List<Replacement> replacements) throws IOException {
        PipedInputStream in = new PipedInputStream();
        final Future[] future = new Future[1];
        PipedOutputStream out = new PipedOutputStream(in) {
            @Override
            public void close() throws IOException {
                super.close();
                try {
                    future[0].get();
                } catch (ExecutionException | InterruptedException e) {
                    throw new IOException(e);
                }
            }
        };

        JsonFilter filter = new JsonFilter(in, from, replacements);

        future[0] = executor.submit(filter);
        return out;
    }



    static class JsonFilter implements Callable<Void> {
        static JsonFactory factory = new JsonFactory();
        static {
            factory.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
            factory.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
            factory.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
            factory.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
        }

        final InputStream in;
        final OutputStream out;
        final List<Replacement> replacements;

        public JsonFilter(InputStream in, OutputStream out, List<Replacement> replacements) throws IOException {
            this.in = in;
            this.out = out;
            this.replacements = replacements;
        }


        private <T> T handleReplacements(Deque<String> stack, T value) {
            String fieldName = stack.poll();
            for (Replacement replacement : replacements) {
                if (replacement.key.equals(fieldName)) {
                    if (replacement.value.equals(value)) {
                        return (T) replacement.newValue;
                    }
                }
            }
            return value;
        }

        @Override
        public Void  call() throws IOException {
            final JsonParser parser = factory.createParser(in);
            final JsonGenerator generator = factory.createGenerator(out);
            Deque<String> stack = new ArrayDeque<>();
            while (true) {
                JsonToken token = parser.nextToken();
                if (token == null) {
                    break;
                }
                switch (token) {
                    case START_OBJECT:
                        generator.writeStartObject();
                        break;
                    case END_OBJECT:
                        generator.writeEndObject();
                        break;
                    case START_ARRAY:
                        generator.writeStartArray();
                        break;
                    case END_ARRAY:
                        generator.writeEndArray();
                        break;
                    case FIELD_NAME: {
                        String fieldName = parser.getText();
                        generator.writeFieldName(fieldName);
                        stack.push(fieldName);
                        break;
                    }
                    case VALUE_EMBEDDED_OBJECT:
                        //generator.writeObject();
                        stack.poll();
                        break;
                    case VALUE_STRING:
                        generator.writeString(handleReplacements(stack, parser.getText()));
                        break;
                    case VALUE_NUMBER_INT:
                        generator.writeNumber(handleReplacements(stack, parser.getValueAsInt()));
                        break;
                    case VALUE_NUMBER_FLOAT:
                        generator.writeNumber(handleReplacements(stack, parser.getValueAsInt()));
                        break;
                    case VALUE_TRUE:
                        generator.writeBoolean(true);
                        stack.poll();
                        break;
                    case VALUE_FALSE:
                        generator.writeBoolean(false);
                        stack.poll();
                        break;
                    case VALUE_NULL:
                        generator.writeNull();
                        stack.poll();
                        break;

                }
            }
            generator.close();
            return null;
        }

    }
    static class Replacement<T> {
        String key;
        T value;
        T newValue;
    }
}
