/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.client.resteasy3;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;


/**
 * @author Michiel Meeuwissen
 */
public class AcceptLanguageRequestFilter implements ClientRequestFilter {

    private final List<Locale> locale;

    public AcceptLanguageRequestFilter(List<Locale> locale) {
        this.locale = locale;
    }

    public AcceptLanguageRequestFilter(Locale... locale) {
        this(Arrays.asList(locale));
    }

    @Override
    public void filter(ClientRequestContext requestContext) {
        if (locale != null) {
            List<String> localesAsString = locale.stream().map(Locale::toString).collect(Collectors.toList());
            requestContext.getHeaders()
                .putSingle(HttpHeaders.ACCEPT_LANGUAGE, localesAsString.stream()
                    .collect(Collectors.joining(", ")));

        }
    }

}
