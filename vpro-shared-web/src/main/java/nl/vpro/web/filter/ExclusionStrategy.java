package nl.vpro.web.filter;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

public interface ExclusionStrategy {
    boolean exclude(HttpServletRequest request);
    void setServletContext(ServletContext servletContext);
}
