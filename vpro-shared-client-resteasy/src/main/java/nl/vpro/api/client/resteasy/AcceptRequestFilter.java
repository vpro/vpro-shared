/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.client.resteasy;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;


/**
 * @author Michiel Meeuwissen
 */
public class AcceptRequestFilter implements ClientRequestFilter {

    private final MediaType mediaType;

    public AcceptRequestFilter(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        if (mediaType != null) {
            List<Object> current = requestContext.getHeaders().get(HttpHeaders.ACCEPT);
            if (current != null) {
                current.add(0, mediaType.toString());
            } else {
                requestContext.getHeaders().add(HttpHeaders.ACCEPT, mediaType.toString());

            }
        }
    }

}
