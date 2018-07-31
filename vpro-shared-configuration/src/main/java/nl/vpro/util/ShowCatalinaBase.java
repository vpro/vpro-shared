package nl.vpro.util;

import java.io.File;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.commons.lang.StringUtils;

/**
 * Sometimes it is hard to know what currently is 'catalina.base' (intellij...) what we often are
 * interested in, because logs are to be found relative to this.
 * @author Michiel Meeuwissen
 * @since 1.74
 */
public class ShowCatalinaBase implements Consumer<Map<String, String>> {

    private static boolean shown = false;



    @Override
    public void accept(Map<String, String> stringStringMap) {
         if (! shown) {
            String catalinaBase = System.getProperty("catalina.base");
            if (StringUtils.isNotEmpty(catalinaBase)) {
                System.out.println("CATALINA BASE:\n'" + catalinaBase + File.separator + "'");
            }
            shown = true;
        }

    }
}
