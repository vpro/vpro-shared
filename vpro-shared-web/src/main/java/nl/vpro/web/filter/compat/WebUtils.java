package nl.vpro.web.filter.compat;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

/**
 * Stripped-down version of the WebUtils class in Spring Framework 2.5, so this
 * module doesn't depend on Spring 2.5 (www-main is still on Spring 1.x).
 * 
 * @author nils
 * 
 */
public abstract class WebUtils {
	/**
	 * Retrieve the first cookie with the given name. Note that multiple cookies
	 * can have the same name but different paths or domains.
	 * 
	 * @param request
	 *            current servlet request
	 * @param name
	 *            cookie name
	 * @return the first cookie with the given name, or <code>null</code> if
	 *         none is found
	 */
	public static Cookie getCookie(HttpServletRequest request, String name) {
		if (request == null) {
			throw new IllegalArgumentException("Request must not be null");
		}
		Cookie cookies[] = request.getCookies();
		if (cookies != null) {
			for (int i = 0; i < cookies.length; i++) {
				if (name.equals(cookies[i].getName())) {
					return cookies[i];
				}
			}
		}
		return null;
	}
}
