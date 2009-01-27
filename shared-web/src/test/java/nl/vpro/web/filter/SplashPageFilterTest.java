package nl.vpro.web.filter;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SplashPageFilterTest {
	
	//@Test
	public void testIsSplashPageActive() {
		HttpServletRequest request = createMock(HttpServletRequest.class);
		HttpServletResponse response = createMock(HttpServletResponse.class);

		expect(request.getParameter(isA(String.class))).andStubReturn(createMock(Cookie.class));
		
		replay(request);
		
		// test
		
		verify(request);
	}
}
