package nl.vpro.swagger;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.ws.rs.core.MediaType;

import nl.vpro.jackson2.JsonFilter;
import nl.vpro.web.HttpServletRequestUtils;

/**
 * This filter can be used to fill in 'api.basePath' using the request, so you don't have to configure it any more.
 * @author Michiel Meeuwissen
 * @since 0.21
 */
@Slf4j
public class SwaggerFilter implements Filter {

    private static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();

    @Override
    public void init(FilterConfig filterConfig) {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;

        if (! req.getPathInfo().endsWith(".json")) {
            String accept = req.getHeader("accept");
            if (accept != null) {
                boolean json = false;
                try {
                    String[] mtypes = accept.split(";", 2)[0].split(",");
                    for (String mtype : mtypes) {
                        if (MediaType.valueOf(mtype).isCompatible(MediaType.APPLICATION_JSON_TYPE)) {
                            json = true;
                            break;
                        }
                    }
                } catch (Exception e) {
                    log.warn(e.getMessage());
                }
                if (!json) {
                    log.debug("Not json");
                    chain.doFilter(request, response);
                    return;
                }
            }
        }
        String scheme = req.getHeader("X-Forwarded-Proto");
        long serverPort = req.getServerPort();
        if (scheme == null) {
            scheme = req.getScheme();
        } else {
            switch(scheme) {
                case "http": serverPort = 80; break;
                case "https": serverPort = 443; break;
            }
        }

        String host = req.getServerName() + ":" + serverPort;
        StringBuilder newValue = HttpServletRequestUtils.getContextURL(req);

		newValue.append("/api");

        JsonFilter.Replacement<String> replacementBasePath =
				new JsonFilter.Replacement<>("basePath", "${api.basePath}", newValue.toString());

        JsonFilter.Replacement<String> replacementHost =
				new JsonFilter.Replacement<>("host", "${api.host}", host);
        List<JsonFilter.Replacement<?>> replacements = Arrays.asList(replacementBasePath, replacementHost);
        final ServletOutputStream servletOutputStream = response.getOutputStream();
        final OutputStream out = transform(servletOutputStream, replacements);
        HttpServletResponseWrapper wrapped = new HttpServletResponseWrapper((HttpServletResponse) response) {
            @Override
            public ServletOutputStream getOutputStream() {
                return new ServletOutputStream() {
                    @Override
                    public boolean isReady() {
                        return servletOutputStream.isReady();

                    }

                    @Override
                    public void setWriteListener(WriteListener writeListener) {
                        servletOutputStream.setWriteListener(writeListener);
                    }

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
        EXECUTOR_SERVICE.shutdownNow();

    }

    public OutputStream transform(OutputStream from, List<nl.vpro.jackson2.JsonFilter.Replacement<?>> replacements) throws IOException {
        PipedInputStream in = new PipedInputStream();
        final Future<?>[] future = new Future[1];
        PipedOutputStream out = new PipedOutputStream(in) {
            @Override
            public void close() throws IOException {
                super.close();
                try {
                    future[0].get();
                } catch (ExecutionException e) {
                    throw new IOException(e);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException(e);

                }
            }
        };

        JsonFilter filter = new JsonFilter(in, from, replacements);

        future[0] = EXECUTOR_SERVICE.submit(filter);
        return out;
    }



}
