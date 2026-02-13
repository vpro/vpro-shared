
package nl.vpro.jmx;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import javax.management.*;

import static nl.vpro.util.ReflectionUtils.classForName;
import static nl.vpro.util.ReflectionUtils.defaultGetter;

/**
 * Provides support for the annotation {@link Description}, {@link Name} and, {@link Units}. {@link MBeans#registerBean(ObjectName, Object)}, wraps mbeans in this,
 * so that the proper metadata of them is visible in mbean clients (like visualvm).
 *
 * See <a href="http://actimem.com/java/jmx-annotations/">here</a>, from which this class was largely copied.
 */

public class AnnotatedStandardMXBean extends StandardMBean {

    public <T> AnnotatedStandardMXBean(T implementation, Class<T> mbeanInterface) {
        super(implementation, mbeanInterface, true);
    }

    @Override
    protected String getDescription(MBeanInfo beanInfo) {
        Description annotation = getMBeanInterface().getAnnotation(Description.class);
        if (annotation!=null) {
            return annotation.value();
        }
        return beanInfo.getDescription();
    }

    /**
     * Returns the description of an Attribute
     */
    @Override
    protected String getDescription(MBeanAttributeInfo info) {
        String attributeName = info.getName();
        String getterName = defaultGetter(attributeName);
        Method m = methodByName(getMBeanInterface(), getterName);
        if (m != null) {
            Description d = m.getAnnotation(Description.class);
            if (d != null) {
                return d.value();
            }
        }
        return info.getDescription();
    }

    /**
     * Returns the description of an operation
     */
    @Override
    protected String getDescription(MBeanOperationInfo op) {
        Method m = methodByOperation(getMBeanInterface(), op);
        Units units =  null;
        if (m != null) {
            Description d = m.getAnnotation(Description.class);
            units = m.getAnnotation(Units.class);
            if (d != null) {
                return d.value() + getUnitsDescription(units);
            }
        }
        return op.getDescription() + getUnitsDescription(units);
    }

    /**
     * Returns the description of a parameter
     */
    @Override
    protected String getDescription(MBeanOperationInfo op, MBeanParameterInfo param, int paramNo) {
        Method m = methodByOperation(getMBeanInterface(), op);
        Units units =  null;
        if (m != null) {
            Description description = getParameterAnnotation(m, paramNo, Description.class);

            units = getParameterAnnotation(m, paramNo, Units.class);

            if (description != null) {
                return description.value() + getUnitsDescription(units);
            }
        }
        return getParameterName(op, param, paramNo) + getUnitsDescription(units);
    }

    private String getUnitsDescription(Units units) {
         return (units == null ? "" : (" in " + units.value()));
    }

    /**
     * Returns the name of a parameter
     */
    @Override
    protected String getParameterName(MBeanOperationInfo op, MBeanParameterInfo param, int paramNo) {
        Method m = methodByOperation(getMBeanInterface(), op);
        if (m != null) {
            Name pname = getParameterAnnotation(m, paramNo, Name.class);
            if (pname != null) {
                return pname.value();
            }
        }
        return param.getName();
    }

    /**
     * Returns the annotation of the given class for the method.
     */
    private static <A extends Annotation> A getParameterAnnotation(Method m, int paramNo, Class<A> annotation) {
        for (Annotation a : m.getParameterAnnotations()[paramNo]) {
            if (annotation.isInstance(a)) {
                return annotation.cast(a);
            }
        }
        return null;
    }

    /**
     * Finds a method within the interface using the method's name and parameters
     */
    private static Method methodByName(Class<?> mbeanInterface, String name, String... paramTypes) {
        try {
            final ClassLoader loader = mbeanInterface.getClassLoader();
            final Class<?>[] paramClasses = new Class<?>[paramTypes.length];
            for (int i = 0; i < paramTypes.length; i++) {
                paramClasses[i] = classForName(paramTypes[i], loader);
            }
            return mbeanInterface.getMethod(name, paramClasses);
        } catch (RuntimeException e) {
            // avoid accidentally catching unexpected runtime exceptions
            throw e;
        } catch (Exception e) {
            return null;
        }
    }


    /**
     * Returns the method from the interface for the given bean operation info.
     */
    private static Method methodByOperation(Class<?> mbeanInterface, MBeanOperationInfo op) {
        final MBeanParameterInfo[] params = op.getSignature();
        final String[] paramTypes = new String[params.length];
        for (int i = 0; i < params.length; i++) {
            paramTypes[i] = params[i].getType();
        }

        return methodByName(mbeanInterface, op.getName(), paramTypes);
    }

}
