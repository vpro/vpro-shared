package nl.vpro.jpa;

import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.hibernate.dialect.HSQLDialect;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hsqldb.jdbc.JDBCDataSource;
import org.junit.Ignore;
import org.junit.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

/**
 * @author Michiel Meeuwissen
 * @since 1.55
 */
public class JpaTest {


    @Test
    @Ignore("Requires a postgresql instance")
    public void testPostgresql() {
        testManager(setupPostgresql());
    }

    @Test
    public void testHsql() {
        testManager(setupHsql());
    }

    protected EntityManagerFactory setupPostgresql() {

        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setDatabaseName("test");
        dataSource.setUser("media");
        dataSource.setPassword("media");

        Properties properties = new Properties();
        properties.put("hibernate.dialect", org.hibernate.dialect.PostgreSQL82Dialect.class);
        properties.put("hibernate.connection.driver_class", org.postgresql.Driver.class);
        properties.put("hibernate.hbm2ddl.auto", "create-drop");

        return  createFactory(dataSource, properties);
    }
    public EntityManagerFactory setupHsql() {

        JDBCDataSource dataSource = new JDBCDataSource();
        dataSource.setUrl("jdbc:hsqldb:mem:testDB");
        dataSource.setUser("sa");
        dataSource.setPassword("");
        Properties properties = new Properties();

        properties.put("hibernate.dialect", HSQLDialect.class);
        properties.put("hibernate.connection.driver_class", org.hsqldb.jdbc.JDBCDriver.class);
        properties.put("hibernate.hbm2ddl.auto", "create");
        return createFactory(dataSource, properties);


    }

    protected EntityManagerFactory createFactory(DataSource dataSource, Properties properties) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("nl.vpro.jpa");
        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        em.setJpaProperties(properties);
        em.setPersistenceUnitName("abcde-domain");
        em.setPersistenceProviderClass(HibernatePersistenceProvider.class);
        em.afterPropertiesSet();

        return em.getObject();
    }


    protected void testManager(EntityManagerFactory factory) {
        EntityManager manager = factory.createEntityManager();
        manager.getTransaction().begin();
        manager.merge(new D());
        manager.merge(new E());
        manager.getTransaction().commit();
        manager.close();
    }
}
