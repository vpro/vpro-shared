package nl.vpro.jpa;

import lombok.extern.log4j.Log4j2;

import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Michiel Meeuwissen
 * @since 6.0
 */
@Log4j2
@SpringJUnitConfig(HsqldbJpaTestConfig.class)
public class HsqldbJpaTest extends JpaTest {

}
