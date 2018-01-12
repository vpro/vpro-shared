package nl.vpro.web.filter;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

public interface ExclusionStrategy {
    public boolean exclude(HttpServletRequest request) throws ServletException;
    public void setServletContext(ServletContext servletContext);
}   
