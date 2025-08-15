package nl.vpro.web.filter;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;

/**
 * This exclusion strategy is a workaround for an issue with a filter being
 * mapped to "/" did not work. This strategy does the URL filtering now to
 * accomplish the desired result.
 *
 * @author nils
 *
 */
public class RootOnlyStrategy implements ExclusionStrategy {

    @Override
    public boolean exclude(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        return !requestURI.equals("/");
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
    }
}
