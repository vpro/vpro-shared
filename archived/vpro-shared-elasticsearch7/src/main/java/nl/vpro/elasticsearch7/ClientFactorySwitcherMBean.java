package nl.vpro.elasticsearch7;

/**
 * @author Michiel Meeuwissen
 * @since 0.48
 */
public interface ClientFactorySwitcherMBean {

    void setConfigured(String configured);

    String getConfigured();
}
