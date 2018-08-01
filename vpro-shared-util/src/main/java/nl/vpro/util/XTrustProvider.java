package nl.vpro.util;

import lombok.extern.slf4j.Slf4j;

import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.function.Consumer;

import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactorySpi;
import javax.net.ssl.X509TrustManager;

// Source: http://devcentral.f5.com/weblogs/joe/archive/2005/07/06/1345.aspx
/*
 * The contents of this file are subject to the "END USER LICENSE AGREEMENT FOR F5
 * Software Development Kit for iControl"; you may not use this file except in
 * compliance with the License. The License is included in the iControl
 * Software Development Kit.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is iControl Code and related documentation
 * distributed by F5.
 *
 * Portions created by F5 are Copyright (C) 1996-2004 F5 Networks
 * Inc. All Rights Reserved.  iControl (TM) is a registered trademark of
 * F5 Networks, Inc.
 *
 * Alternatively, the contents of this file may be used under the terms
 * of the GNU General Public License (the "GPL"), in which case the
 * provisions of GPL are applicable instead of those above.  If you wish
 * to allow use of your version of this file only under the terms of the
 * GPL and not to allow others to use your version of this file under the
 * License, indicate your decision by deleting the provisions above and
 * replace them with the notice and other provisions required by the GPL.
 * If you do not delete the provisions above, a recipient may use your
 * version of this file under either the License or the GPL.
 */

/**
 * Use <code>XTrustProvider.install()</code> and you don't have any dealings with untrusted certificates any more....
 */
@Slf4j
public final class XTrustProvider extends java.security.Provider {

    private final static String NAME = "XTrustJSSE";
    private final static String INFO = "XTrust JSSE Provider (implements trust factory with truststore validation disabled)";
    private final static double VERSION = 1.0D;

    public XTrustProvider() {
        super(NAME, VERSION, INFO);

        AccessController.doPrivileged((PrivilegedAction) () -> {
            put("TrustManagerFactory." + TrustManagerFactoryImpl.getAlgorithm(), TrustManagerFactoryImpl.class.getName());
            return null;
        });
    }

    public static void trustAll(boolean trustAll) {
        if (trustAll) {
            install();
        }
    }

    public static void install() {
        if (Security.getProvider(NAME) == null) {
            Security.insertProviderAt(new XTrustProvider(), 2);
            Security.setProperty("ssl.TrustManagerFactory.algorithm", TrustManagerFactoryImpl.getAlgorithm());
            log.info("Trusting all certificates");
        }
    }

    public static class Installer implements Consumer<Map<String, String>> {


        final String prop;

        public Installer(String prop) {
            this.prop = prop;
        }


        @Override
        public void accept(Map<String, String> map) {

            if (Boolean.parseBoolean(map.get(prop))) {
                install();
            }
        }
    }


    public final static class TrustManagerFactoryImpl extends TrustManagerFactorySpi {
        public TrustManagerFactoryImpl() {
        }

        public static String getAlgorithm() {
            return "XTrust509";
        }

        @Override
        protected void engineInit(KeyStore keystore) throws KeyStoreException {
        }

        @Override
        protected void engineInit(ManagerFactoryParameters mgrparams) throws InvalidAlgorithmParameterException {
            throw new InvalidAlgorithmParameterException(XTrustProvider.NAME + " does not use ManagerFactoryParameters");
        }

        @Override
        protected TrustManager[] engineGetTrustManagers() {
            return new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    @Override
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
            };
        }
    }
}

