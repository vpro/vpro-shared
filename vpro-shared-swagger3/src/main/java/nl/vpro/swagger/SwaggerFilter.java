package nl.vpro.swagger;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.util.regex.Pattern;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.ws.rs.core.MediaType;

import org.meeuw.json.grep.Sed;
import org.meeuw.json.grep.matching.*;

import nl.vpro.web.HttpServletRequestUtils;

/**
 * This filter can be used to fill in 'api.basePath' using the request, so you don't have to configure it any more.
 * @author Michiel Meeuwissen
 * @since 0.21
 */
@Slf4j
public class SwaggerFilter implements Filter {


    @Override
    public void init(FilterConfig filterConfig) {

    }

    @Override
    public void doFilter(
        ServletRequest request,
        ServletResponse response, FilterChain chain) throws IOException, ServletException {
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

        PathMatcher matcher = getPathMatcher(req);

        final ServletOutputStream servletOutputStream = response.getOutputStream();
        final OutputStream out = transform(servletOutputStream, matcher);
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


    PathMatcher getPathMatcher(HttpServletRequest req) {
        String host = req.getServerName() + HttpServletRequestUtils.getPortPostFixIfNeeded(req);
        String basePath = req.getContextPath() + "/api";
        return getPathMatcher(basePath, host, req.getContextPath());
    }

    PathMatcher getPathMatcher(String basePath, String host, String context) {
        return new PathMatcherOrChain(
            new PathMatcherAndChain(
                new SinglePathMatcher(new PreciseMatch("basePath")),
                new ScalarEqualsMatcher("${api.basePath}", basePath)
            ),
            new PathMatcherAndChain(
                new SinglePathMatcher(new PreciseMatch("basePath")),
                new ScalarRegexpMatcher(Pattern.compile("[/]?(.*)\\$\\{CONTEXT}(.*)"), "$1" + context +"$2")
            ),
            new PathMatcherAndChain(
                new SinglePathMatcher(new PreciseMatch("host")),
                new ScalarEqualsMatcher("${api.host}", host)
            )
        );
    }

    public OutputStream transform(OutputStream to, PathMatcher pathMatcher) throws IOException {
        return Sed.transform(to, pathMatcher);
    }



}
