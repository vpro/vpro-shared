package nl.vpro.web.filter;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;


/**
 * 
 * @author nils
 *
 */
public class SplashPageFilterTest {
	
	private SplashPageFilter splashPagefilter;
	private HttpServletRequest request;
	private HttpServletResponse response;
	private FilterChain chain;
	private FilterConfig filterConfig;
	
	@Before
	public void setUp() {
		request = createMock(HttpServletRequest.class);
		response = createMock(HttpServletResponse.class);
		chain = createMock(FilterChain.class);
		filterConfig = createMock(FilterConfig.class);
		splashPagefilter = new SplashPageFilter();
	}
	
	@Test
	public void testDoFilterWithoutNosplashParameter() throws IOException, ServletException {
		expect(request.getRequestURI()).andReturn("/");
		expect(request.getParameter("nosplash")).andReturn(null);
		expect(filterConfig.getInitParameter("start")).andReturn("2008-01-01 12:00");
		expect(filterConfig.getInitParameter("end")).andReturn("3008-01-01 12:00");
		expect(filterConfig.getInitParameter("target")).andReturn("url");
		expect(filterConfig.getInitParameter("cookiename")).andReturn(null);
		expect(filterConfig.getInitParameter("requestFilterClass")).andReturn(null);
		expect(request.getCookies()).andReturn(new Cookie[]{});
		response.addCookie(isA(Cookie.class));
		response.sendRedirect("url");
		replay(request, response, filterConfig, chain);
		splashPagefilter.init(filterConfig);
		splashPagefilter.doFilter(request, response, chain);
		verify(response);
	}
	
	@Test
	public void testDoFilterWithCookieSet() throws IOException, ServletException {
		expect(request.getRequestURI()).andReturn("/");
		expect(request.getParameter("nosplash")).andReturn(null);
		expect(filterConfig.getInitParameter("start")).andReturn("2008-01-01 12:00");
		expect(filterConfig.getInitParameter("end")).andReturn("3008-01-01 12:00");
		expect(filterConfig.getInitParameter("target")).andReturn("url");
		expect(filterConfig.getInitParameter("cookiename")).andReturn("skipSplashPage");
		expect(filterConfig.getInitParameter("requestFilterClass")).andReturn(null);
		expect(request.getCookies()).andReturn(new Cookie[]{ new Cookie("skipSplashPage", Boolean.toString(true)) });
		chain.doFilter(request, response);
		replay(request, response, filterConfig, chain);
		splashPagefilter.init(filterConfig);
		splashPagefilter.doFilter(request, response, chain);
		verify(chain);
	}
}
