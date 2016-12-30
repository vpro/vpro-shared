package nl.vpro.web.filter;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

/**
 * This exclusion strategy is a workaround for an issue with a filter being
 * mapped to "/" did not work. This strategy does the URL filtering now to
 * accomplish the desired result.
 * 
 * @author nils
 * 
 */
public class RootOnlyStrategy implements ExclusionStrategy {

	public boolean exclude(HttpServletRequest request) throws ServletException {
		String requestURI = request.getRequestURI();
		return !requestURI.equals("/");
	}

	public void setServletContext(ServletContext servletContext) {
	}
}
