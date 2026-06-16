package nl.vpro.jpa;

import java.util.Properties;

import javax.sql.DataSource;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.jpa.HibernatePersistenceProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Abstract base Spring context configuration for JPA tests.
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "nl.vpro.jpa")
public abstract class JpaTestConfig {

    /** Subclasses provide database-specific JPA/Hibernate properties. */
    protected abstract Properties jpaProperties();

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("nl.vpro.jpa");
        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        em.setPersistenceUnitName("abcde-domain");
        em.setPersistenceProviderClass(HibernatePersistenceProvider.class);
        em.setJpaProperties(jpaProperties());
        return em;
    }

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
