/**
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.client.resteasy;

import java.io.IOException;
import java.util.Locale;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;


/**
 * @author Michiel Meeuwissen
 */
public class AcceptLanguageRequestFilter implements ClientRequestFilter {

    private final Locale locale;

    public AcceptLanguageRequestFilter(Locale locale) {
        this.locale = locale;
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        requestContext.getAcceptableLanguages().add(locale);
    }

}
