/**
 * Copyright (C) 2011 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.apache.ws.security;

import javax.annotation.Resource;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.IOException;

import org.apache.ws.security.WSPasswordCallback;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class ServerCallbackHandler implements CallbackHandler {

    @Resource(name = "authenticationManager")
    AuthenticationManager manager;

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for(Callback callback : callbacks) {
            WSPasswordCallback wsp = (WSPasswordCallback)callback;
            switch(wsp.getUsage()) {
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
        }
    }
}
