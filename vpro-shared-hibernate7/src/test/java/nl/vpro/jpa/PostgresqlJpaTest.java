package nl.vpro.jpa;

import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * @author Michiel Meeuwissen
 * @since 6.0
 */
@Testcontainers
@SpringJUnitConfig(classes = PostgresqlJpaTestConfig.class)
public class PostgresqlJpaTest extends JpaTest {






}
