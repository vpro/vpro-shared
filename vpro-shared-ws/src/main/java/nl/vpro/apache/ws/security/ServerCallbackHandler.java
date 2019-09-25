/*
 * Copyright (C) 2011 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.apache.ws.security;

import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Slf4j
public class ServerCallbackHandler implements CallbackHandler {

    @Resource(name = "authenticationManager")
    AuthenticationManager manager;

    @Override
    public void handle(Callback[] callbacks) throws UnsupportedCallbackException {
        for(Callback callback : callbacks) {
            try {
                WSPasswordCallback wsp = (WSPasswordCallback) callback;
                switch (wsp.getUsage()) {
                    case WSPasswordCallback.USERNAME_TOKEN:
                        final String name = wsp.getIdentifier();
                        final String password = wsp.getPassword();

                        Authentication authentication = new UsernamePasswordAuthenticationToken(name, password);
                        authentication = manager.authenticate(authentication);

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        break;
                    default:
                        throw new UnsupportedCallbackException(wsp);
                }
            } catch (RuntimeException re) {
                log.error(re.getMessage(), re);
                throw re;
            }
        }
    }
}
