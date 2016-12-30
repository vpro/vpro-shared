package nl.vpro.apache.ws.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Helper class for retrieving userID from spring security
 * @author Danny Sedney
 *
 */
public class PrincipalHelper {
	/**
	 * returns the current thread's principal
	 */
	public static String getPrincipal() {
		SecurityContext ctx = SecurityContextHolder.getContext();
		if (ctx != null) {
			Authentication auth = ctx.getAuthentication();
			if (auth != null) {
				return auth.getName();
			}
		}
		// not set (no project specific system exception available, so throw RuntimeException)
		throw new RuntimeException("No security principal set");
	}
	
}
