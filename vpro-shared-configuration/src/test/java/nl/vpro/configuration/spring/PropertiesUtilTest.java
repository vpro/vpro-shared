package nl.vpro.configuration.spring;

import lombok.extern.slf4j.Slf4j;

import java.net.UnknownHostException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Michiel Meeuwissen
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration
@Slf4j
public class PropertiesUtilTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    ConfigurableBeanFactory beanFactory;

    @Value("${http.host}")
    private String test;

    @Value("${system}")
    private String system;


    @Test
    public void testGetMap() {
        PropertiesUtil properties = applicationContext.getBean(PropertiesUtil.class);
        assertThat(properties.getMap().get("b")).isEqualTo("B");
        assertThat(properties.getMap().get("c")).isEqualTo("A/B/C");
    }

    @Test
    public void testEL() throws UnknownHostException {

        log.info("{} {}", test, system);
        System.setProperty("some.system.property", "foobar");
        PropertiesUtil properties = applicationContext.getBean(PropertiesUtil.class);
        String v = properties.getMap().get("http.host");
        assertThat(v).isEqualTo(java.net.InetAddress.getLocalHost().getHostName());
        assertThat(properties.getMap().get("http.host.tooverride")).isEqualTo("michiel.vpro.nl");
        assertThat(properties.getMap().get("url")).isEqualTo("http://" +java.net.InetAddress.getLocalHost().getHostName());

        assertThat(properties.getMap().get("url.tooverride")).isEqualTo("http://michiel.vpro.nl");

        // TODO: these fail:
        //assertThat(properties.getMap().get("system")).isEqualTo("foobar");
        //assertThat(properties.getMap().get("maybesystem")).isEqualTo("foobar");
        log.info("{}", properties.getMap());

    }

    @SuppressWarnings("deprecation")
    @Test
    public void defaultSpring() {

        log.info("{}", test);
        System.setProperty("some.system.property", "foobar");
        org.springframework.beans.factory.config.PropertyPlaceholderConfigurer properties = (PropertyPlaceholderConfigurer) applicationContext.getBean("defaultspring");
        log.info("{}", properties);

    }

    @Test
    public void testSetExposeAsSystemProperty() {
        assertThat(System.getProperty("b")).isEqualTo("B");
        assertThat(System.getProperty("a")).isNull();
        assertThat(System.getProperty("c")).isNull();

    }
}
