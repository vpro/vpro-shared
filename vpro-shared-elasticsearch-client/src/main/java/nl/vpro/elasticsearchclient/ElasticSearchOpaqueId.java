package nl.vpro.elasticsearchclient;

import org.apache.logging.log4j.ThreadContext;

import nl.vpro.logging.mdc.MDCConstants;

public final class ElasticSearchOpaqueId {

    private ElasticSearchOpaqueId() {
    }

    /**
     * Adds the current user and HTTP method and path, when present, without forwarding query parameters.
     */
    public static String withRequest(String operation) {
        String request = ThreadContext.get(MDCConstants.REQUEST);
        String userName = ThreadContext.get(MDCConstants.USER_NAME);
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
