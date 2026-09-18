package nl.vpro.elasticsearchclient;

import org.apache.logging.log4j.ThreadContext;

public final class ElasticSearchOpaqueId {

    private static final String REQUEST_MDC_KEY = "request";
    private static final String USER_NAME_MDC_KEY = "userName";

    private ElasticSearchOpaqueId() {
    }

    /**
     * Adds the current user and HTTP method and path, when present, without forwarding query parameters.
     */
    public static String withRequest(String operation) {
        String request = ThreadContext.get(REQUEST_MDC_KEY);
        String userName = ThreadContext.get(USER_NAME_MDC_KEY);
        StringBuilder result = new StringBuilder(operation == null ? "" : operation);
        if (userName != null && !userName.isBlank()) {
            append(result, "user=" + userName);
        }
        if (request != null && !request.isBlank()) {
            int queryStart = request.indexOf('?');
            append(result, (queryStart < 0 ? request : request.substring(0, queryStart)).trim());
        }
        return result.isEmpty() ? null : result.toString();
    }

    private static void append(StringBuilder result, String value) {
        if (!result.isEmpty()) {
            result.append(' ');
        }
        result.append(value);
    }
}
