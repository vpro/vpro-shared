package nl.vpro.rs.media;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;

/**
 * MSE-4330. Er was gewoon een index.jsp die dat deed, maar dat werkt niet meer, omdat om een of andere reden het resteasy servlet er altijd tussen zat (terwijl niet eens gemapt)
 * @author Michiel Meeuwissen
 * @since 2.6
 */
@Slf4j
public class WelcomeFilter implements Filter {



    @Override
    public void init(FilterConfig filterConfig) {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException {
        ((HttpServletResponse)servletResponse).sendRedirect("docs/api");

    }

    @Override
    public void destroy() {

    }
}
