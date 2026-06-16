package nl.vpro.jpa;

import java.util.Properties;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import nl.vpro.test.psql.PostgresqlContainerSupport;

/**
 * Spring context configuration for PostgreSQL-based JPA tests.
 * The DataSource and PostgreSQLContainer beans are provided by {@link PostgresqlContainerSupport}.
 */
@Configuration
@Import(PostgresqlContainerSupport.class)
public class PostgresqlJpaTestConfig extends JpaTestConfig {

    @Override
    protected Properties jpaProperties() {
        Properties properties = new Properties();
        properties.put("hibernate.connection.driver_class", org.postgresql.Driver.class);
        properties.put("hibernate.hbm2ddl.auto", "create-drop");
        return properties;
    }
}
