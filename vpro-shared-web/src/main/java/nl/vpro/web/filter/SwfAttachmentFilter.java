package nl.vpro.web.filter;

import java.io.IOException;
import java.util.Arrays;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.commons.lang.StringUtils;

/**
 * <a href="http://www.adobe.com/devnet/flashplayer/articles/fplayer10_security_changes_02.html#head32"
 * >http://www.adobe.com/devnet/flashplayer/articles/ fplayer10_security_changes_02.html#head32</a> describes a change
 * in the flash player that ignores any flash content that is served with a Content-Disposition header with the value
 * 'attachment'. This filter will remove that header for these specific attachments.
 * 
 * 
 * @author Auke van Leeuwen
 * @author Peter Maas
 */
public class SwfAttachmentFilter implements Filter {
  private static final String CONTENT_DISPOSITION_HEADER = "Content-Disposition";
  private static final String PARAM_MIMETYPE = "mimetypes";
  private static final String PARAM_EXTENSION = "extensions";
  private static final String DEFAULT_MIMETYPES = "application/x-shockwave-flash";
  private static final String DEFAULT_EXTENSIONS = "swf,flv";
  private static final String PARAM_SPLIT_REGEX = ",[\\s]*";

  private String[] mimetypes;
  private String[] extensions;

  /** {@inheritDoc} */
  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
    final HttpServletRequest request = (HttpServletRequest) req;
    final HttpServletResponse response = (HttpServletResponse) res;

    HttpServletResponseWrapper responseWrapper = new HttpServletResponseWrapper(response) {
      @Override
      public void setHeader(String name, String value) {
        if (CONTENT_DISPOSITION_HEADER.equalsIgnoreCase(name) && shouldFilterRequest(request, response)) {
          // simply ignore the setting of the header.
          return;
        }

        super.setHeader(name, value);
      }
    };

    chain.doFilter(req, responseWrapper);
  }

  private boolean shouldFilterRequest(HttpServletRequest request, HttpServletResponse response) {
    // the charset may be returned in the contenttype as well
    String contentType = response.getContentType().split(";")[0];
    boolean matchesMimeType = Arrays.binarySearch(mimetypes, contentType) >= 0;

    String extension = StringUtils.defaultString(request.getPathInfo()).replaceFirst("^.*\\.([^$]*)$", "$1");
    boolean matchesExtension = Arrays.binarySearch(extensions, extension) >= 0;

    return matchesMimeType || matchesExtension;
  }

  /** {@inheritDoc} */
  public void init(FilterConfig config) throws ServletException {
    String mimetypeParameter = StringUtils.defaultString(config.getInitParameter(PARAM_MIMETYPE), DEFAULT_MIMETYPES);
    String extensionParameter = StringUtils.defaultString(config.getInitParameter(PARAM_EXTENSION), DEFAULT_EXTENSIONS);
    mimetypes = mimetypeParameter.split(PARAM_SPLIT_REGEX);
    extensions = extensionParameter.split(PARAM_SPLIT_REGEX);

    // sort so we can do a binarysearch
    Arrays.sort(mimetypes);
    Arrays.sort(extensions);
  }

  /** {@inheritDoc} */
  public void destroy() {
  }
}
