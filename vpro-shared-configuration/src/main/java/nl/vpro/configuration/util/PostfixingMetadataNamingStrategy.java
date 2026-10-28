package nl.vpro.configuration.util;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.jmx.export.naming.MetadataNamingStrategy;
import org.springframework.jmx.support.ObjectNameManager;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
public class PostfixingMetadataNamingStrategy extends MetadataNamingStrategy {

    private final String postfix;

    public PostfixingMetadataNamingStrategy(String prefix) {
        this.postfix = prefix;
    }

    @NonNull
    @Override
    public ObjectName getObjectName(Object managedBean, @Nullable String beanKey) throws MalformedObjectNameException {
        ObjectName name = super.getObjectName(managedBean, beanKey);
        return ObjectNameManager.getInstance(name.getDomain() + "." + postfix, name.getKeyPropertyList());
    }
}
