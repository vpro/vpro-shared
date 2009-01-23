package nl.vpro.mmbase.applications.portalettes;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

public interface RequestFilter {
    public boolean pass(HttpServletRequest request) throws ServletException;
    public void setServletContext(ServletContext servletContext);
}   
