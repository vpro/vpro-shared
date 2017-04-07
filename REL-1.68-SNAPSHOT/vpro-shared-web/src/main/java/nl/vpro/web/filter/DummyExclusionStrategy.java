package nl.vpro.web.filter;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

/**
 * dummy implementation that passes all requests
 * @author ernst
 *
 */
public class DummyExclusionStrategy implements ExclusionStrategy {

    public boolean exclude(HttpServletRequest request) {
        return false;
    }

    public void setServletContext(ServletContext servletContext) {
    }

}
