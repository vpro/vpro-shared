package nl.vpro.web.filter;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;

/**
 * dummy implementation that passes all requests
 * @author ernst
 *
 */
public class DummyExclusionStrategy implements ExclusionStrategy {

    @Override
    public boolean exclude(HttpServletRequest request) {
        return false;
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
    }

}
