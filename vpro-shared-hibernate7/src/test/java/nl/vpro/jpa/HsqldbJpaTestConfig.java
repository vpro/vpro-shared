package nl.vpro.jpa;

import java.util.Properties;

import javax.sql.DataSource;

import org.hsqldb.jdbc.JDBCDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring context configuration for HSQLDB-based JPA tests.
 */
@Configuration
public class HsqldbJpaTestConfig extends JpaTestConfig {

    @Bean
    public DataSource dataSource() {
        JDBCDataSource dataSource = new JDBCDataSource();
        dataSource.setUrl("jdbc:hsqldb:mem:testDB");
        dataSource.setUser("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    @Override
    protected Properties jpaProperties() {
        Properties properties = new Properties();
        properties.put("hibernate.connection.driver_class", org.hsqldb.jdbc.JDBCDriver.class);
        properties.put("hibernate.hbm2ddl.auto", "create");
        return properties;
    }
}

