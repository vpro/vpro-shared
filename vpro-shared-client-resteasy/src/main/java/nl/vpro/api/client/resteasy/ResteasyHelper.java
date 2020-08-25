package nl.vpro.api.client.resteasy;

import lombok.SneakyThrows;

import javax.ws.rs.client.ClientRequestFilter;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.slf4j.Logger;

/**
 * Some reflection to make things work with both Resteasy 3, and Resteasy 4 (similar as keycloak client is doing)
 * @author Michiel Meeuwissen
 * @since 2.14
 */
public class ResteasyHelper {

    private ResteasyHelper() {

    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    public static ResteasyClientBuilder clientBuilder() {

        try {
            // Resteasy 4
            Class<? extends ResteasyClientBuilder>  clazz = (Class<? extends ResteasyClientBuilder>) Class.forName("org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl");
            return clazz.newInstance();
        } catch (ClassNotFoundException cnf) {
            Class<? extends ResteasyClientBuilder>  clazz = (Class<? extends ResteasyClientBuilder>) Class.forName("org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder");
            return (ResteasyClientBuilder) clazz.getMethod("newBuilder").invoke(null);
        }

    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    public static ClientRequestFilter getBasicAuthentication(String userName, String password, Logger log) {
        Class<? extends ClientRequestFilter> clazz;
        try {
            // Resteasy 4
            clazz = (Class<? extends ClientRequestFilter>) Class.forName("org.jboss.resteasy.client.jaxrs.internal.BasicAuthentication");
        } catch (ClassNotFoundException e) {
            log.error(e.getMessage(), e);
            try {
                // Resteasy 3
                clazz = (Class<? extends ClientRequestFilter>) Class.forName("org.jboss.resteasy.client.jaxrs.BasicAuthentication");
            } catch (ClassNotFoundException ignore) {
                throw new RuntimeException("No Resteasy 3 nor Resteassy basic authentiation found");

            }
        }
        return clazz.getConstructor(String.class, String.class).newInstance(userName, password);
    }

    @SneakyThrows
    public static ClientHttpEngine createApacheHttpClient(CloseableHttpClient closeableHttpClient, boolean autoClose) {
        try {
            Class<?> clazz = Class.forName("org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClientEngine");
            return (ClientHttpEngine) clazz.getMethod("create", CloseableHttpClient.class, boolean.class).invoke(null, closeableHttpClient, autoClose);
        } catch (ClassNotFoundException cnf) {
            Class<?> clazz = Class.forName("org.jboss.resteasy.client.jaxrs.engines.factory.ApacheHttpClient4EngineFactory");
            return (ClientHttpEngine) clazz.getMethod("create", HttpClient.class, boolean.class).invoke(null, closeableHttpClient, autoClose);

        }
    }
}


