package nl.vpro.jmx;

import java.util.Map;

import javax.management.MXBean;

/**
 * @author Michiel Meeuwissen
 * @since 2.12
 */
@MXBean
public interface MBeansUtilsMXBean {

    @Description("Cancel jmx process with key")
    String cancel(@Name("key") String key);

    @Description("Running")
    String waitFor(@Name("key") String key);

    @Description("Running")
    Map<String, String> getRunning();


}
