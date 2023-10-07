/*
 * Copyright (C) 2011 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.apache.ws.security;

import java.io.IOException;

import jakarta.security.auth.callback.*;

import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.springframework.security.core.context.SecurityContextHolder;

// Does not work since Spring 3.1
public class ClientCallbackHandler implements CallbackHandler {

    @Override
    public void handle(Callback[] callbacks) throws UnsupportedCallbackException {
        for (Callback callback : callbacks) {
            WSPasswordCallback wsp = (WSPasswordCallback) callback;
            switch (wsp.getUsage()) {
                case WSPasswordCallback.USERNAME_TOKEN:
                    // Does not work since Spring 3.1
                    String password = (String) SecurityContextHolder.getContext().getAuthentication().getCredentials();
                    wsp.setPassword(password);
                    break;
                default:
                    throw new UnsupportedCallbackException(wsp);
            }
        }
    }
}
