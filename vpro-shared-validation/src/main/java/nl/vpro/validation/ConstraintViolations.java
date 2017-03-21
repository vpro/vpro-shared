package nl.vpro.validation;

import java.util.ArrayList;
import java.util.List;

import javax.validation.ConstraintViolation;

import com.google.common.base.Joiner;

/**
 * @author Michiel Meeuwissen
 * @since 3.4
 */
public class ConstraintViolations {

    public static  <T extends Object> String humanReadable(Iterable<ConstraintViolation<T>> violations) {
        StringBuilder builder = new StringBuilder();
        List<String> propsWithHtml = new ArrayList<>();
        for (ConstraintViolation<?> violation : violations) {
            if (violation.getConstraintDescriptor().getAnnotation().annotationType().isAssignableFrom(NoHtml.class)) {
                String prop = violation.getPropertyPath().toString();
                if (! propsWithHtml.contains(prop)) { // e.g. sometimes several descriptions have the same error.
                    propsWithHtml.add(prop);
                }
            } else {
                builder.append("\n").append(violation.getPropertyPath()).append(" ").append(violation.getMessage());
            }
        }
        if (! propsWithHtml.isEmpty()) {
            builder.append('"').append(Joiner.on(", ").join(propsWithHtml)).append("\" contains HTML");
        }
        return builder.toString();
    }
}
