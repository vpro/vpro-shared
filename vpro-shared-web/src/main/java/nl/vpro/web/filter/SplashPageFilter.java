package nl.vpro.web.filter;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.vpro.web.filter.compat.ServletRequestUtils;
import nl.vpro.web.filter.compat.WebUtils;


/**
 * Determines whether or not a splash page should be shown.
 *
 * @author Auke van Leeuwen
 * @author Ernst Bunders
 */
public class SplashPageFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(SplashPageFilter.class);

    private static final String PARAM_REQUEST_FILTER_CLASS = "requestFilterClass";

    private static final String NOSPLASH_PARAM = "nosplash";

    private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd HH:mm";

    private static final String DEFAULT_COOKIE_NAME = "skipSplashPage";

    private String cookieName;

    private Date startDate;

    private Date endDate;

    private String target;

    private ExclusionStrategy exclusionStrategy = new DummyExclusionStrategy();

    private String exclusionStrategyClassName;

    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest)req;
        HttpServletResponse response = (HttpServletResponse)resp;

        log.debug("Request URI: " + request.getRequestURI());

        if(applyFilter(request, response)) {
            Cookie skipSplashCookie = new Cookie(cookieName, Boolean.toString(true));
            int secondsTillEndDate = (int)((endDate.getTime() - new Date().getTime()) / 1000);
            skipSplashCookie.setMaxAge(secondsTillEndDate);
            response.addCookie(skipSplashCookie);
            response.sendRedirect(target);
        } else {
            chain.doFilter(request, response);
        }
    }

    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("Starting SplashPage filter.");

        // required init parameters
        String start = filterConfig.getInitParameter("start");
        String end = filterConfig.getInitParameter("end");
        target = filterConfig.getInitParameter("target");
        if(StringUtils.isBlank(start) || StringUtils.isBlank(end) || StringUtils.isBlank(target)) {
            throw new ServletException("Can't start filter! required parameters (start, end, target) not found.");
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_PATTERN);
        try {
            startDate = dateFormat.parse(start);
            endDate = dateFormat.parse(end);
        } catch(ParseException e) {
            throw new ServletException("Could not parse date: " + e);
        }

        // optional init parameters
        cookieName = filterConfig.getInitParameter("cookiename");
        if(cookieName == null) {
            cookieName = DEFAULT_COOKIE_NAME;
        }

        exclusionStrategyClassName = filterConfig.getInitParameter(PARAM_REQUEST_FILTER_CLASS);
        if(!StringUtils.isBlank(exclusionStrategyClassName)) {
            try {
                if(isRequestFilter(exclusionStrategyClassName)) {
                    exclusionStrategy = (ExclusionStrategy)Class.forName(exclusionStrategyClassName).newInstance();
                    ServletContext servletContext = filterConfig.getServletContext();
                    exclusionStrategy.setServletContext(servletContext);
                }
            } catch(Exception e) {
                throw new ServletException("could not instantiate RequestFilter from classname: " + exclusionStrategyClassName, e);
            }
        }
        log.info(String.format("Applying filter from %s to %s", startDate, endDate));
    }

    private boolean isRequestFilter(String requestFilterClassName) throws ClassNotFoundException {
        Class<?> c = this.getClass().getClassLoader().loadClass(requestFilterClassName);
        return ExclusionStrategy.class.isAssignableFrom(c);
    }

    public void destroy() {
        log.info("Destroying SplashPage Filter.");
    }

    private boolean applyFilter(HttpServletRequest request, HttpServletResponse response) {
        return isSplashPageActive(request) && withinDateRange() && cookieNotSet(request) && notExcludedByStrategy(request);
    }

    private boolean isSplashPageActive(HttpServletRequest request) {
        return !ServletRequestUtils.getBooleanParameter(request, NOSPLASH_PARAM, false);
    }

    private boolean withinDateRange() {
        Date now = new Date();
        return now.after(startDate) && now.before(endDate);
    }

    private boolean cookieNotSet(HttpServletRequest request) {
        return WebUtils.getCookie(request, cookieName) == null;
    }

    private boolean notExcludedByStrategy(HttpServletRequest request) {
        return !exclusionStrategy.exclude(request);
    }
}
