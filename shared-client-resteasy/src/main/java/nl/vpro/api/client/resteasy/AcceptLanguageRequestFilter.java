/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.client.resteasy;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.HttpHeaders;


/**
 * @author Michiel Meeuwissen
 */
public class AcceptLanguageRequestFilter implements ClientRequestFilter {

    private final List<Locale> locale;

    public AcceptLanguageRequestFilter(List<Locale> locale) {
        this.locale = locale;
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        if (locale != null) {
            List<Object> current = requestContext.getHeaders().get(HttpHeaders.ACCEPT_LANGUAGE);
            List<String> localesAsString = locale.stream().map(Locale::toString).collect(Collectors.toList());
            if (current != null) {
                current.addAll(0, localesAsString);
            } else {
                requestContext.getHeaders().addAll(HttpHeaders.ACCEPT_LANGUAGE, localesAsString);

            }
        }
    }

}
