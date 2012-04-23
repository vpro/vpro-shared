/**
 * Copyright (C) 2011 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.apache.ws.security;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.ws.security.WSPasswordCallback;

public class StaticPasswordCallbackHandler implements CallbackHandler {

    private final String password;

    public StaticPasswordCallbackHandler(String password) {
        this.password = password;
    }

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (Callback callback : callbacks) {
            WSPasswordCallback wsp = (WSPasswordCallback) callback;
            switch (wsp.getUsage()) {
                case WSPasswordCallback.USERNAME_TOKEN:
                    wsp.setPassword(password);
                    break;
                default:
                    throw new UnsupportedCallbackException(wsp);
            }
        }
    }
}
