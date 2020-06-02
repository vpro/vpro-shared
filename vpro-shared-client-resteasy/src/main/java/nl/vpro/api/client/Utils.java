package nl.vpro.api.client;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import javax.ws.rs.*;

/**
 * @author Michiel Meeuwissen
 * @since 2.12
 */
public class Utils {

    /**
     * Creates a string representation of a call to a javax.ws.rs annotated method.
     * So, it considers annotations like {@link Path}, {@link PathParam} and {@link QueryParam} to
     * create an accurate yet clear presentation of what happened or will happen.
     */
    public static String methodCall(Method method, Object[] args) {
        StringBuilder pathBuilder = new StringBuilder();
        {
            Path classPath = method.getClass().getAnnotation(Path.class);
            if ( classPath != null) {
                pathBuilder.append(classPath.value());
            }
        }
        {
            Path path = method.getAnnotation(Path.class);
            if ( path != null) {
                pathBuilder.append(path.value());
            }
        }
        StringBuilder queryBuilder = new StringBuilder();
        StringBuilder bodyBuilder = new StringBuilder();


        {
            Annotation[][] parameterAnnotations = method.getParameterAnnotations();
            char sep = '?';
            OUTER:
            for (int i = 0; i <  parameterAnnotations.length; i++) {
                for (int j = 0 ; j < parameterAnnotations[i].length; j++) {
                    Annotation a = parameterAnnotations[i][j];
                    if (a instanceof PathParam) {
                        pathBuilder.append('/').append(args[i]).append('/');
                        continue OUTER;
                    }
                    if (a instanceof QueryParam) {
                        if (args[i] != null) {
                            queryBuilder.append(sep).append(((QueryParam) a).value()).append('=').append(args[i]);
                            sep = '&';
                        }
                        continue OUTER;
                    }
                }
                if (args[i] != null) {
                    bodyBuilder.append("\t");
                    bodyBuilder.append(args[i]);
                }

            }
        }
        pathBuilder.append(queryBuilder);
        pathBuilder.append(bodyBuilder);
        return pathBuilder.toString();
    }

}
