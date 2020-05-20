package nl.vpro.configuration.spring;

import lombok.extern.slf4j.Slf4j;

import java.net.UnknownHostException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Michiel Meeuwissen
 * @since ...
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


    @Test
    public void testGetMap() {
        PropertiesUtil properties = applicationContext.getBean(PropertiesUtil.class);

        assertThat(properties.getMap().get("b")).isEqualTo("B");
        assertThat(properties.getMap().get("c")).isEqualTo("A/B/C");



    }

    @Test
    public void testEL() throws UnknownHostException {

        log.info("{}", test);
        PropertiesUtil properties = applicationContext.getBean(PropertiesUtil.class);
        String v = properties.getMap().get("http.host");
        assertThat(v).isEqualTo(java.net.InetAddress.getLocalHost().getHostName());

    }

    @Test
    public void testSetExposeAsSystemProperty() {


        assertThat(System.getProperty("b")).isEqualTo("B");
        assertThat(System.getProperty("a")).isNull();
        assertThat(System.getProperty("c")).isNull();

    }
}
