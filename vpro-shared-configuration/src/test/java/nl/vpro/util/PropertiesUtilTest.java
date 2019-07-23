package nl.vpro.util;

import lombok.extern.slf4j.Slf4j;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.expression.StandardBeanExpressionResolver;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Michiel Meeuwissen
 * @since ...
 */
@RunWith(SpringJUnit4ClassRunner.class)
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
    public void testEL() {

        log.info("{}", test);
        PropertiesUtil properties = applicationContext.getBean(PropertiesUtil.class);
        String v = properties.getMap().get("http.host");
        StandardBeanExpressionResolver resolver = new StandardBeanExpressionResolver();

        ExpressionParser parser = new SpelExpressionParser();
        resolver.setExpressionParser(parser);;
        //Object evaluate = beanFactory.getBeanExpressionResolver().evaluate(v, beanFactory.)

        //log.info("{}", evaluate);

    }

    @Test
    public void testSetExposeAsSystemProperty() {


        assertThat(System.getProperty("b")).isEqualTo("B");
        assertThat(System.getProperty("a")).isNull();
        assertThat(System.getProperty("c")).isNull();

    }
}
