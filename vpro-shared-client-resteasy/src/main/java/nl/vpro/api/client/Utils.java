package nl.vpro.api.client;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import javax.ws.rs.*;

/**
 * @author Michiel Meeuwissen
 * @since 2.12
 */
public class Utils {
      
    public static String methodCall(Method method, Object[] args) {
        StringBuilder builder = new StringBuilder();
        {
            Path classPath = method.getClass().getAnnotation(Path.class);
            if ( classPath != null) {
                builder.append(classPath.value());
            }
        }
        {
            Path path = method.getAnnotation(Path.class);
            if ( path != null) {
                builder.append(path.value());
            }
        }
        {
            Annotation[][] parameterAnnotations = method.getParameterAnnotations();
            for (int i = 0; i <  parameterAnnotations.length; i++) {
                for (int j = 0 ; j < parameterAnnotations[i].length; j++) {
                    Annotation a = parameterAnnotations[i][j];
                    if (a instanceof PathParam) {
                        builder.append('/').append(args[i]).append('/');
                    }
                    if (a instanceof QueryParam) {
                        if (args[i] != null) {
                            builder.append('&').append(((QueryParam) a).value()).append('=').append(args[i]);
                        }
                    }
                }

            }
        }
        return builder.toString();
    }

}
