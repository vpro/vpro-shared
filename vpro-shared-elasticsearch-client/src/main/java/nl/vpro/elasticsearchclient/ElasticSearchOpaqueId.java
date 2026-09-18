package nl.vpro.elasticsearchclient;

import org.apache.logging.log4j.ThreadContext;

public final class ElasticSearchOpaqueId {

    private static final String REQUEST_MDC_KEY = "request";

    private ElasticSearchOpaqueId() {
    }

    /**
     * Adds the current HTTP method and path, when present, without forwarding query parameters.
     */
    public static String withRequest(String operation) {
        String request = ThreadContext.get(REQUEST_MDC_KEY);
        if (request == null || request.isBlank()) {
            return operation;
        }
        int queryStart = request.indexOf('?');
        String methodAndPath = (queryStart < 0 ? request : request.substring(0, queryStart)).trim();
        return operation == null || operation.isBlank()
            ? methodAndPath
            : operation + " " + methodAndPath;
    }
}
