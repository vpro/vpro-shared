package nl.vpro.jpa;

import java.util.Properties;

import javax.sql.DataSource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import nl.vpro.test.psql.PostgresqlContainerSupport;

import org.hibernate.jpa.HibernatePersistenceProvider;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * @author Michiel Meeuwissen
 * @since 6.0
 */
@Testcontainers
public class PostgresqlJpaTest extends JpaTest {


    static PostgreSQLContainer container = new PostgreSQLContainer("postgres:15.3")
        .withDatabaseName("test")
        .withUsername("media")
        .withPassword("media");
    static {
        container.start();
    }


    @Test
    public void testPostgresql() {
        testManager(setupPostgresql());
    }


    protected EntityManagerFactory setupPostgresql() {

        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setServerNames(new String[] {container.getHost()});
        dataSource.setPortNumbers(new int[] {container.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT)});
        dataSource.setDatabaseName("test");
        dataSource.setUser("media");
        dataSource.setPassword("media");

        Properties properties = new Properties();
        //properties.put("hibernate.dialect", org.hibernate.dialect.PostgreSQLDialect.class);
        properties.put("hibernate.connection.driver_class", org.postgresql.Driver.class);
        properties.put("hibernate.hbm2ddl.auto", "create-drop");

        return  createFactory(dataSource, properties);
    }



}
