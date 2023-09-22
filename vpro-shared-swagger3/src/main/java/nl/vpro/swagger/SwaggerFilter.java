package nl.vpro.swagger;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

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
public class SwaggerFilter implements Filter {


    boolean filterAlways = false;
    String restPrefix = "/";
    @Override
    public void init(FilterConfig filterConfig) {
        filterAlways = "true".equals(filterConfig.getInitParameter("filterAlways"));
        restPrefix = Optional.ofNullable(filterConfig.getServletContext().getInitParameter("resteasy.servlet.mapping.prefix")).orElse(restPrefix);

    }

    @Override
    public void doFilter(
        ServletRequest request,
        ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String accept = req.getHeader(HttpHeaders.ACCEPT);

        boolean yaml = SwaggerListingResource.isYaml(parseAcceptHeader(accept));

        if (yaml) {
            log.debug("Not json, because yaml");
            chain.doFilter(request, response);
            return;
        }
        if (! filterAlways) {
            // only filter if path ends with .json
            if (req.getPathInfo() == null || ! req.getPathInfo().endsWith(".json")) {
                // not surely json. Just skip.
                chain.doFilter(request, response);
                return;
            }
        }

        final PathMatcher matcher = getPathMatcher(req);

        final ServletOutputStream servletOutputStream = response.getOutputStream();
        final OutputStream out = transform(servletOutputStream, matcher);
        final HttpServletResponseWrapper wrapped = new HttpServletResponseWrapper((HttpServletResponse) response) {
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

    PathMatcher getPathMatcher(String basePath, String host) {
        return new PathMatcherOrChain(
            new PathMatcherAndChain(
                new SinglePathMatcher(new PreciseMatch("servers"), new ArrayEntryMatch(), new PreciseMatch("url")),
                new ScalarEqualsMatcher("${api.basePath}", host + basePath)
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
