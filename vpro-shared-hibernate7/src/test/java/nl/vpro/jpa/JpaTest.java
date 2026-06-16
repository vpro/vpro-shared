package nl.vpro.jpa;

import lombok.extern.log4j.Log4j2;

import java.net.URI;
import java.util.Properties;
import java.util.UUID;

import javax.sql.DataSource;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.jpa.HibernatePersistenceProvider;
import org.junit.jupiter.api.Test;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 1.55
 */
@Log4j2
public abstract class JpaTest {

    @Inject
    protected DRepository repository;

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

    @Test
    protected void testRepository() {
        D d = new D();
        d.setUuidField(UUID.randomUUID());
        d.setUuidField(UUID.randomUUID());
        d.setUriField(URI.create("http://example.com"));
        d.setAField("a");
        d.setA3Field("a3");
        d.setFalseToNull(false);
        D saved = repository.save(d);

        D found = repository.findById(saved.getId()).orElseThrow();
        log.info("Found: {}", found);
        log.info("All: {}", repository.findAll());

        assertThat(d.getUuidField()).isEqualTo(found.getUuidField());
        assertThat(d.getUriField()).isEqualTo(found.getUriField());
        assertThat(found.getFalseToNull()).isNull();
    }


    protected void testManager(EntityManagerFactory factory) {
        EntityManager manager = factory.createEntityManager();
        manager.getTransaction().begin();
        D d = new D();
        d.setUuidField(UUID.randomUUID());
        manager.merge(new D());
        manager.merge(new E());

        manager.getTransaction().commit();
        manager.close();
    }
}
