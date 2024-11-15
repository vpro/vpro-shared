package nl.vpro.swagger;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.apache.commons.text.StringSubstitutor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.meeuw.json.grep.Sed;
import org.meeuw.json.grep.matching.*;

import nl.vpro.web.HttpServletRequestUtils;

/**
 * This filter can be used to fill in 'api.basePath' using the request, so you don't have to configure it anymore.
 * @author Michiel Meeuwissen
 * @since 0.21
 */
@Slf4j
public class SwaggerFilter extends HttpFilter {


    boolean filterAlways = false;
    String restPrefix = "/";
    Map<String, String> parameters = Map.of();
    @Override
    public void init(FilterConfig filterConfig) {
        filterAlways = "true".equals(filterConfig.getInitParameter("filterAlways"));

        Map<String, String> map = new HashMap<>();
        filterConfig.getInitParameterNames().asIterator().forEachRemaining((parameter) ->
            map.put(parameter, filterConfig.getInitParameter(parameter))
        );
        parameters = Collections.unmodifiableMap(map);
        restPrefix = Optional.ofNullable(filterConfig.getServletContext().getInitParameter("resteasy.servlet.mapping.prefix")).orElse(restPrefix);

    }

    @Override
    public void doFilter(
        HttpServletRequest request,
        HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        String accept = request.getHeader(HttpHeaders.ACCEPT);

        boolean yaml = SwaggerListingResource.isYaml(parseAcceptHeader(accept));

        if (yaml) {
            log.debug("Not json, because yaml");
            chain.doFilter(request, response);
            return;
        }
        if (! filterAlways) {
            // only filter if path ends with .json
            if (request.getPathInfo() == null || ! request.getPathInfo().endsWith(".json")) {
                // not surely json. Just skip.
                chain.doFilter(request, response);
                return;
            }
        }

        final PathMatcher matcher = getPathMatcher(request);

        final ServletOutputStream servletOutputStream = response.getOutputStream();
        final OutputStream out = transform(servletOutputStream, matcher);
        final HttpServletResponseWrapper wrapped = new HttpServletResponseWrapper(response) {
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


    PathMatcher getPathMatcher(HttpServletRequest req) {
        String host = req.getScheme() + "://" + req.getServerName() + HttpServletRequestUtils.getPortPostFixIfNeeded(req);
        String basePath = req.getContextPath() + restPrefix;
        return getPathMatcher(basePath, host);
    }


    private String substituteParameters(String in) {
        return StringSubstitutor.replace(in, parameters, "${", "}");
    }

    PathMatcher getPathMatcher(String basePath, String host) {
        return new PathMatcherOrChain(
            new PathMatcherAndChain(
                new SinglePathMatcher(new PreciseMatch("servers"), new ArrayEntryMatch(), new PreciseMatch("url")),
                new ScalarEqualsMatcher("${api.basePath}", host + basePath)
            ),
            // The job of replacing ${baseUrl} can actually also be done by nl.vpro.swagger.OpenAPIApplication.fixDocumentation(io.swagger.v3.oas.models.ExternalDocumentation)
            new PathMatcherAndChain(
                new SinglePathMatcher(
                    new PreciseMatch("externalDocs"),
                    new PreciseMatch("url")),
                new ReplaceScalarMatcher(this::substituteParameters)
            ),
            new PathMatcherAndChain(
                new SinglePathMatcher(true,
                    new PreciseMatch("tags"),
                    new PreciseMatch("externalDocs"),
                    new PreciseMatch("url")),
                new ReplaceScalarMatcher(this::substituteParameters)
            )
        );
    }

    public OutputStream transform(OutputStream to, PathMatcher pathMatcher) throws IOException {
        return Sed.transform(to,
            pathMatcher
        );
    }

    public static List<MediaType> parseAcceptHeader(@Nullable String accept) {
        List<MediaType> result = new ArrayList<>();
        if (accept != null) {
            String[] mtypes = accept.split(";", 2)[0].split(",");
            for (String mtype : mtypes) {
                result.add(MediaType.valueOf(mtype));
            }
        }
        return result;
    }

}
