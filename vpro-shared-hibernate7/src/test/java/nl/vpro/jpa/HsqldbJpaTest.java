package nl.vpro.jpa;

import java.util.Properties;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.dialect.HSQLDialect;
import org.hsqldb.jdbc.JDBCDataSource;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * @author Michiel Meeuwissen
 * @since 6.0
 */

public class HsqldbJpaTest extends JpaTest {



    @Test
    public void testHsql() {
        testManager(setupHsql());
    }
    public EntityManagerFactory setupHsql() {

        JDBCDataSource dataSource = new JDBCDataSource();
        dataSource.setUrl("jdbc:hsqldb:mem:testDB");
        dataSource.setUser("sa");
        dataSource.setPassword("");
        Properties properties = new Properties();

        //properties.put("hibernate.dialect", HSQLDialect.class);
        properties.put("hibernate.connection.driver_class", org.hsqldb.jdbc.JDBCDriver.class);
        properties.put("hibernate.hbm2ddl.auto", "create");
        return createFactory(dataSource, properties);
    }





}
