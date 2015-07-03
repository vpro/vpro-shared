/**
 * Copyright (C) 2011 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.apache.ws.security;

import javax.annotation.Resource;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.message.token.UsernameToken;
import org.apache.wss4j.dom.validate.UsernameTokenValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;

public class PlainTextUsernameTokenValidator extends UsernameTokenValidator {

    private static final Logger LOG = LoggerFactory.getLogger(PlainTextUsernameTokenValidator.class);


    @Resource(name = "authenticationManager")
    AuthenticationManager manager;

    @Override
    protected void verifyPlaintextPassword(UsernameToken usernameToken, RequestData data) throws WSSecurityException {
        final String name = usernameToken.getName();
        final String password = usernameToken.getPassword();

        Authentication authentication = new UsernamePasswordAuthenticationToken(name, password);
        try {
            authentication = manager.authenticate(authentication);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (AuthenticationException ae) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION, name + ":" + ae.getMessage(), ae);
        } catch (RuntimeException t) {
            LOG.error(t.getMessage(), t);
            throw t;
        }
    }
}
