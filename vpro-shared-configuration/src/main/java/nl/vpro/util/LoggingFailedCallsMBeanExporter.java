package nl.vpro.util;

import lombok.extern.slf4j.Slf4j;

import javax.management.MBeanException;
import javax.management.ReflectionException;
import javax.management.modelmbean.ModelMBean;
import javax.management.modelmbean.RequiredModelMBean;

import org.springframework.jmx.export.MBeanExporter;
import org.springframework.jmx.export.SpringModelMBean;

/**
 * Exceptions from spring managed mbeans are silently swallowed. They are thrown to the clients (like visualvm), but they more often then not don't know how to gracefully show them
 * (e.g. I think it is required that the exception is serializable then).
 * @author Michiel Meeuwissen
 * @since 1.75
 */
@Slf4j
public class LoggingFailedCallsMBeanExporter extends MBeanExporter {

    @Override
    protected ModelMBean createModelMBean() throws MBeanException {
        ModelMBean superModelMBean = super.createModelMBean();

        // but this.exposeManagedResourceClassLoader is not visible, so we switch on the type of the returned ModelMBean
        if (superModelMBean instanceof SpringModelMBean) {
            return new SpringModelMBean() {
                @Override
                public Object invoke(String opName, Object[] opArgs, String[] sig) throws MBeanException, ReflectionException {
                    try {
                        return super.invoke(opName, opArgs, sig);
                    } catch (MBeanException | ReflectionException | Error | RuntimeException e) {
                        log.error(e.getMessage(), e);
                        throw e;
                    }
                }
            };
        } else {
            return new RequiredModelMBean() {
                @Override
                public Object invoke(String opName, Object[] opArgs, String[] sig) throws MBeanException, ReflectionException {
                    try {
                        return super.invoke(opName, opArgs, sig);
                    } catch (MBeanException | ReflectionException | RuntimeException | Error e) {
                        log.error(e.getMessage(), e);
                        throw e;
                    }
                }
            };
        }
    }


}
