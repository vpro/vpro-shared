package nl.vpro.util;

import org.apache.commons.lang.StringUtils;

/**
 * Sometimes it is hard to know what currently is 'catalina.base' (intellij...) what we often are
 * interested in, because logs are to be found relative to this.
 * @author Michiel Meeuwissen
 * @since 1.74
 */
public class ShowCatalinaBase implements Runnable {
    @Override
    public void run() {
        String catalinaBase = System.getProperty("catalina.base");
        if (StringUtils.isNotEmpty(catalinaBase)) {
            System.out.println("CATALINA BASE: '" + catalinaBase + "'");
        }

    }
}
