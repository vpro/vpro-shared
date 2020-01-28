/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.rs.client;

import java.util.*;
import java.util.stream.Collectors;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;


/**
 * Fills the Accept header to enforce content negotation in a certain media type.
 * @author Michiel Meeuwissen
 */
public class AcceptRequestFilter implements ClientRequestFilter {

    private final MediaType mediaType;

    public AcceptRequestFilter(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    @Override
    public void filter(ClientRequestContext requestContext) {
        if (mediaType != null) {
            List<Object> current = requestContext.getHeaders().get(HttpHeaders.ACCEPT);
            if (current != null) {
                if (current.size() > 0) {
                    Map<String, String> lowQuality= new HashMap<>();
                    lowQuality.put("q", "0.5");
                    List<MediaType> mediaTypes = Arrays.stream(((String) current.get(0)).split("\\s*,\\s*"))
                        .map(MediaType::valueOf)
                        .map(mt -> (!mediaType.isCompatible(mt)) ? new MediaType(mt.getType(), mt.getSubtype(), lowQuality) : mt).sorted((o1, o2) -> {
                            boolean b1 = mediaType.isCompatible(o1);
                            boolean b2 = mediaType.isCompatible(o2);
                            return -1 * Boolean.compare(b1, b2);
                        }).collect(Collectors.toList());
                    if (mediaTypes.size() > 0) {
                        current.set(0, mediaTypes.stream().map(MediaType::toString).collect(Collectors.joining(", ")));
                    }
                } else {
                    current.add(mediaType.toString());
                }

            } else {
                requestContext.getHeaders().add(HttpHeaders.ACCEPT, mediaType.toString());

            }
        }
    }

}
