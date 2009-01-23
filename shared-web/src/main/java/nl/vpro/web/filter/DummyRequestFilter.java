package nl.vpro.web.filter;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

/**
 * dummy implementation that passes all requests
 * @author ernst
 *
 */
public class DummyRequestFilter implements RequestFilter {

    public boolean pass(HttpServletRequest request) {
        return true;
    }

    public void setServletContext(ServletContext servletContext) {
    }

}
