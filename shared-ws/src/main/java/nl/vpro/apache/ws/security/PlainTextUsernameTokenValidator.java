/**
 * Copyright (C) 2011 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.apache.ws.security;

import javax.annotation.Resource;

import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.handler.RequestData;
import org.apache.ws.security.message.token.UsernameToken;
import org.apache.ws.security.validate.UsernameTokenValidator;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class PlainTextUsernameTokenValidator extends UsernameTokenValidator {

    @Resource(name = "authenticationManager")
    AuthenticationManager manager;

    @Override
    protected void verifyPlaintextPassword(UsernameToken usernameToken, RequestData data) throws WSSecurityException {
        final String name = usernameToken.getName();
        final String password = usernameToken.getPassword();

        Authentication authentication = new UsernamePasswordAuthenticationToken(name, password);
        authentication = manager.authenticate(authentication);

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
