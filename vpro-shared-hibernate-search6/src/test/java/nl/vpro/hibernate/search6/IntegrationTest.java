package nl.vpro.hibernate.search6;

import java.util.HashMap;

import javax.persistence.*;

import org.hibernate.dialect.PostgreSQL10Dialect;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.postgresql.Driver;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;


@Testcontainers

public class IntegrationTest {

    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
        "postgres:16-alpine"
    );

    static    EntityManager entityManager;

    @BeforeAll
    static void startContainers() {
        postgres.withUsername("admin");
        postgres.withPassword("admin2k");
        postgres.withDatabaseName("CRM");
        postgres.start();

        var properties = new HashMap<String, String>();
        properties.put("jakarta.persistence.jdbc.driver", Driver.class.getName());
        properties.put("jakarta.persistence.jdbc.url", postgres.getJdbcUrl());
        properties.put("jakarta.persistence.jdbc.user", postgres.getUsername());
        properties.put("jakarta.persistence.jdbc.password", postgres.getPassword());
        properties.put("hibernate.dialect", PostgreSQL10Dialect.class.getName());
        properties.put("hibernate.show-sql", "true");
        properties.put("hibernate.hbm2ddl.auto", "create-drop");

        EntityManagerFactory emf = Persistence.createEntityManagerFactory("CRM", properties);


        entityManager = emf.createEntityManager();
    }

    @Test
    public void test() {
        TestEntity merge = entityManager.merge(new TestEntity());
        assertThat(merge.getId()).isNotNull();


    }

}
