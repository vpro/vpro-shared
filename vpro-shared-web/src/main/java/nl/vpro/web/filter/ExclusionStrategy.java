package nl.vpro.web.filter;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;

public interface ExclusionStrategy {
    boolean exclude(HttpServletRequest request);
    void setServletContext(ServletContext servletContext);
}
