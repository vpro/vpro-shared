package nl.vpro.rs.client;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import lombok.Getter;

import java.lang.reflect.*;
import java.net.ConnectException;
import java.net.SocketException;
import java.util.*;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

import nl.vpro.jackson2.Jackson2Mapper;

import static java.util.stream.Collectors.joining;

/**
 * Wraps all calls to log client errors.
 *
 * @author Michiel Meeuwissen
 * @param <T> The class of the proxied object
 * @since 4.2
 */

public class ErrorAspect<T, E> implements InvocationHandler {

    private final Logger log;

    private final T proxied;

    private final Supplier<String> string;

    private final Class<E> errorClass;


    /**
     *
     * @param errorClass The class which an error is tried to be unmarshalled to.
     */
    ErrorAspect(T proxied, Logger log, Supplier<String> string, Class<E> errorClass) {
        this.proxied = proxied;
        this.log = log;
        this.string = string;
        this.errorClass = errorClass;
    }


    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return invoke(proxy, method, args, 0);
    }

    protected Object invoke(Object proxy, Method method, Object[] args, int retryCount) throws Throwable {

        final Throwable t;
        final Message mes;
        final Logger l;
        boolean error;
        try {
            try {
                return method.invoke(proxied, args);
            } catch (InvocationTargetException itc) {
                Throwable throwable = itc;
                while(true) {
                    if (throwable instanceof  InvocationTargetException) {
                        throwable = ((InvocationTargetException) throwable).getTargetException();
                        continue;
                    }
                    if (throwable instanceof UndeclaredThrowableException) {
                        throwable = ((UndeclaredThrowableException) throwable).getUndeclaredThrowable();
                        continue;
                    }
                    break;
                }
                throw throwable;
            }
        } catch (WebApplicationException wea) {
            int status = wea.getResponse().getStatus();
            l = getLogger(status);
            mes = getMessage(wea);
            t = wea;
            if (status == 404) {
                try {
                    Object entity = wea.getResponse().getEntity();
                    if (entity != null && isRecognizedErrorClass(entity.getClass())) {
                        error = false;
                    } else {
                        error = true;
                    }
                } catch (IllegalStateException iae) {
                    error = false;
                }
            } else {
                error = status >= 400;
            }
            wea.getResponse().close();

        } catch (jakarta.ws.rs.ProcessingException pe) {
            Throwable cause = pe.getCause();
            mes = new Message(null, cause.getClass().getName() + " " + cause.getMessage());
            l = log;
            t = pe;
            if (cause instanceof SocketException && "Connection or outbound has closed".equals(cause.getMessage()) && retryCount < 10) {
                String name = method.getDeclaringClass().getSimpleName() + "#" + method.getName();
                log.warn("{}. Retrying {}: {}", cause.getMessage(), retryCount, name);
                Thread.sleep((int) Math.max(1000, Math.pow(2, retryCount) * 100));
                Object result = invoke(proxy, method, args, retryCount + 1);
                log.info("Retry {}: {} successful ", retryCount, name);
                return result;
            }
            error = ! (cause instanceof ConnectException);
        } catch (Throwable e) {
            mes = new Message(null, e.getClass().getName() + " " + e.getMessage());
            t = e;
            l = log;
            error = true;
        }
        if (error) {
            l.error("Error for {}{}(\n{}\n) {}",
                string.get(),
                method.getDeclaringClass().getSimpleName() + "#" + method.getName(),
                args == null ? "(no args)" :
                    Arrays.stream(args)
                        .map(ErrorAspect.this::valueToString)
                        .collect(joining("\n")),
                mes);
        } else {
            l.debug("For {}{}(\n{}\n) {}",
                string.get(),
                method.getDeclaringClass().getSimpleName() + "#" + method.getName(),
                args == null ? "(no args)" :
                    Arrays.stream(args)
                        .filter(Objects::nonNull)
                        .map(ErrorAspect.this::valueToString)
                        .collect(joining("\n")),
                mes);
        }
        if (t instanceof RuntimeException) {
            throw t;
        }
        for (Class<?> et : method.getExceptionTypes()) {
            if (et.isAssignableFrom(t.getClass())) {
                throw t;
            }
        }

        throw new RuntimeException(t);
    }

    Logger getLogger(int status) {
        return LoggerFactory.getLogger(log.getName() + "." + (status / 100) + "." + String.format("%02d", (status % 100)));
    }

    protected static boolean isRecognizedErrorClass(Class<?> clazz) {
        return clazz.getPackage().getName().startsWith("nl.vpro") && clazz.getSimpleName().equals("Error");
    }

    protected static final String[] HEADERS = new String[] {
        "Set-Cookie", // may give information about which backend server was used
        "X-ProxyInstancename",
        "Content-Type",
        // problem may be related to json vs xml?
    };
    protected Message getMessage(WebApplicationException we) {
        StringBuilder mes = new StringBuilder();
        E error = null;
        try {
            Response response = we.getResponse();
            try {
                response.bufferEntity();
            } catch (IllegalStateException ise) {
                log.debug(ise.getMessage());
            }

            if (errorClass != null) {
                try {
                    error = response.readEntity(errorClass);
                    mes.append(getMessage(error));

                } catch (Exception e) {
                    // ignore and marshal to string
                }
            }
            switch (we.getResponse().getStatus()) {
                case 503:
                    mes.append(we.getMessage());

            }
            if (mes.length() == 0) {
                try {
                    String m = response.readEntity(String.class);
                    mes.append(response.getStatus()).append(':').append(m);
                } catch (IllegalStateException is) {
                    mes.append(response.getStatus()).append(':').append(we.getMessage());
                }
            }
            if (we.getResponse().getStatusInfo().getStatusCode() == 500) {
                // log all headers, this should be debuggable as good as possible
                for (Map.Entry<String, List<Object>> s : response.getMetadata().entrySet()) {
                    mes.append("; ");
                    mes.append(s.getKey());
                    mes.append('=');
                    List<Object> value = s.getValue();
                    mes.append(value.stream().map(Objects::toString).collect(joining(",")));
                }
            } else {
                for (String s : HEADERS) {
                    List<Object> v = response.getMetadata().get(s);
                    if (v != null) {
                        mes.append("; ");
                        mes.append(s);
                        mes.append('=');
                        mes.append(v.stream().map(Objects::toString).collect(joining(",")));
                    }
                }
            }
        } catch (IllegalStateException ise) {
            log.warn(we + ": " + ise.getClass().getName() + " " + ise.getMessage(), ise);
            mes.append(we.getMessage());
        } catch (Exception e) {
            log.warn(we + ": " + e.getClass().getName() + " " + e.getMessage());
            mes.append(we.getMessage());
        }

        return new Message(error, mes.toString());
    }


    protected String getMessage(Object o) {
        try {
            Method getMessage = o.getClass().getMethod("getMessage");
            return Objects.toString(getMessage.invoke(o));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return o.toString();
        }
    }


    protected String valueToString(Object o) {
        if (o instanceof String) {
            return o.toString();
        } else {
            try {
                return Jackson2Mapper.getInstance().writeValueAsString(o);
            } catch (JsonProcessingException ignored) {

            }
            return Objects.toString(o);
        }
    }


    @SuppressWarnings("unchecked")
    public static <T, E> T proxyErrors(
        Logger logger,
        Supplier<String> info,
        Class<T> restInterface,
        T service,
        Class<E> errorClass) {
        return (T) Proxy.newProxyInstance(restInterface.getClassLoader(), new Class[]{restInterface},
            new ErrorAspect<>(service, logger, info, errorClass));
    }


    public static <T> T proxyErrors(Logger logger, Supplier<String> info, Class<T> restInterface, T service) {
        return proxyErrors(logger, info, restInterface, service, null);
    }



    @Getter
    public class Message {
        final E error;
        final String message;

        public Message(E error, String message) {
            this.error = error;
            this.message = message;
        }
        @Override
        public String toString() {
            return message;
        }
    }
}

