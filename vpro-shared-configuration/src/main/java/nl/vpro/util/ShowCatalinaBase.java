package nl.vpro.util;

import org.apache.commons.lang.StringUtils;

/**
 * @author Michiel Meeuwissen
 * @since 1.74
 */
public class ShowCatalinaBase implements Runnable {
    @Override
    public void run() {
        String catalinaBase = System.getProperty("catalina.base");
        if (StringUtils.isNotEmpty(catalinaBase)) {
            System.out.println("CATALINA BASE: " + catalinaBase);
        }

    }
}
