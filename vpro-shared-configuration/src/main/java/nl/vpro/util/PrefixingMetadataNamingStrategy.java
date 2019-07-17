package nl.vpro.util;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.jmx.export.naming.MetadataNamingStrategy;
import org.springframework.jmx.support.ObjectNameManager;
import org.springframework.lang.Nullable;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
public class PrefixingMetadataNamingStrategy extends MetadataNamingStrategy {

    private final String postfix;

    public PrefixingMetadataNamingStrategy(String prefix) {
        this.postfix = prefix;
    }

    @NonNull
    @Override
	public ObjectName getObjectName(Object managedBean, @Nullable String beanKey) throws MalformedObjectNameException {
        ObjectName name = super.getObjectName(managedBean, beanKey);
        return ObjectNameManager.getInstance(name.getDomain() + "." + postfix, name.getKeyPropertyList());
    }
}
