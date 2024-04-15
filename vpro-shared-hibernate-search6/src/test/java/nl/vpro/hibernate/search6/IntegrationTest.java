package nl.vpro.hibernate.search6;

import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.hibernate.dialect.PostgreSQL10Dialect;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.postgresql.Driver;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.HashMap;
import java.util.function.Consumer;

import jakarta.persistence.*;


import nl.vpro.hibernate.search6.domain.MyEnum;
import nl.vpro.hibernate.search6.domain.TestEntity;

import static org.assertj.core.api.Assertions.assertThat;


@Testcontainers
@Log4j2
@ToString
public class IntegrationTest {

    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
        "postgres:16-alpine"
    );

    static    EntityManager entityManager;

    @BeforeAll
    static void startContainers() throws IOException {
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
        File index = Files.createTempDirectory(IntegrationTest.class.getSimpleName()).toFile();
        properties.put("hibernate.search.backend.directory.root", index.getAbsolutePath());
        log.info("Indexing to {}", index.getAbsolutePath());
        //properties.put("hibernate.search.default.indexBase", "/tmp/indexes");

        properties.put("hibernate.search.mapping.configurer", MyEntityMapper.class.getName());
        properties.put("hibernate.search.backend.analysis.configurer", MyEntityMapper.class.getName());

        EntityManagerFactory emf = Persistence.createEntityManagerFactory("CRM", properties);



        entityManager = emf.createEntityManager();

        var tr = entityManager.getTransaction();
        tr.begin();
        add(b -> b
            .text("Hello World")
            .myEnum(MyEnum.A)
            .instant(Instant.ofEpochSecond(100)),
            b -> b.text("Goodbye Earth")
                .myEnum(MyEnum.B)
                .instant(Instant.ofEpochSecond(200)),
            b -> b.text("foobar")
                .myEnum(MyEnum.C)
                .instant(null)
        );

        tr.commit();
       /* SearchSession searchSession = Search.session( entityManager );

        MassIndexer indexer = searchSession.massIndexer( TestEntity.class )
        .threadsToLoadObjects( 7 );

        indexer.startAndWait();*/
    }

    @SafeVarargs
    private static void add(Consumer<TestEntity.Builder>... builderConsumers) {
        for (Consumer<TestEntity.Builder> builderConsumer : builderConsumers) {
            TestEntity.Builder builder = TestEntity.builder();
            builderConsumer.accept(builder);
            log.info("Merged {}", entityManager.merge(builder.build()));
        }
    }

    @Test
    public void test() {
        TestEntity test = entityManager.find(TestEntity.class, 1L);
        assertThat(test.getId()).isNotNull();
        assertThat(test.getText()).isEqualTo("Hello World");

        SearchSession searchSession = Search.session(entityManager);

        {
            var list = searchSession.search(TestEntity.class).select(SearchProjectionFactory::id).where(SearchPredicateFactory::matchAll).fetchHits(20);
            assertThat(list).isNotNull();
            assertThat(list).hasSize(3);
        }


        var list = searchSession.search(TestEntity.class)
            .where(f -> f.match().field("text").matching("world"))
            .fetchHits(20);

        assertThat(list).hasSize(1);
        log.info("{}", list);


    }

    @Test
    public void enumField() {
        SearchSession searchSession = Search.session(entityManager);
        var list = searchSession.search(TestEntity.class)
            .where(f -> f.match().field("myEnum").matching("a"))
            .fetchHits(20);

        log.info("{}", list);
    }

    @Test
    public void instantField() {
        SearchSession searchSession = Search.session(entityManager);
        var list = searchSession.search(TestEntity.class)
            .where(f -> f.match().field("instant").matching(Instant.ofEpochSecond(100)))
            .fetchHits(20);

        log.info("{}", list);
    }

}
