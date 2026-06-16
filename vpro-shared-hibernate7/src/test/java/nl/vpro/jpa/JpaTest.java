package nl.vpro.jpa;

import org.hibernate.jpa.HibernatePersistenceProvider;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import java.util.Properties;

import javax.sql.DataSource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * @author Michiel Meeuwissen
 * @since 1.55
 */
@Testcontainers
public abstract class JpaTest {






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
