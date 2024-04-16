package nl.vpro.hibernate.search6;

import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.hibernate.dialect.PostgreSQL10Dialect;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.meeuw.math.time.TestClock;
import org.postgresql.Driver;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

import jakarta.persistence.*;


import nl.vpro.hibernate.search6.domain.*;

import static org.assertj.core.api.Assertions.assertThat;


@Testcontainers
@Log4j2
@ToString
public class IntegrationTest {

    static TestClock clock = new TestClock();
    static {

    }

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
                .text("Hallo Werelden")
                .myEnum(MyEnum.A)
                .instant(clock.instant())
                .list(List.of("a", "b", "c"))
            ,
            b -> b.text("Tot ziens aardes!")
                .myEnum(MyEnum.B)
                .instant(clock.instant())
                .subObject(SubObject.builder().a("foo").build())
            ,

            b -> b.text("foobar")
                .myEnum(MyEnum.C)
                .instant(null)
        );

        tr.commit();

        //fullIndex();
    }

    private static void fullIndex() throws InterruptedException {
          SearchSession searchSession = Search.session( entityManager );

        MassIndexer indexer = searchSession.massIndexer( TestEntity.class )
        .threadsToLoadObjects( 7);
        indexer.startAndWait();
    }

    @SafeVarargs
    private static void add(Consumer<TestEntity.Builder>... builderConsumers) {
        for (Consumer<TestEntity.Builder> builderConsumer : builderConsumers) {
            TestEntity.Builder builder = TestEntity.builder();
            builderConsumer.accept(builder);
            var test = builder.build();
            var sub = test.getSubObject();
            if (sub != null) {
                sub = entityManager.merge(sub);
                test.setSubObject(sub);
            }
            log.info("Merged {}", entityManager.merge(test));
        }
    }

    @Test
    public void test() {
        TestEntity test = entityManager.find(TestEntity.class, 1L);
        assertThat(test.getId()).isNotNull();
        assertThat(test.getText()).isEqualTo("Hallo Werelden");

        SearchSession searchSession = Search.session(entityManager);

        {
            var list = searchSession.search(TestEntity.class).select(SearchProjectionFactory::id).where(SearchPredicateFactory::matchAll).fetchHits(20);
            assertThat(list).isNotNull();
            assertThat(list).hasSize(3);
        }


        var list = searchSession.search(TestEntity.class)
            .where(f -> f.match().field("text")
                .matching("wereld")) // wont' work without dutch analyzer
            .fetchAll();

        assertThat(list.hits()).hasSize(1);
        log.info("{}", list);


    }

    @Test
    public void enumField() {
        var searchSession = Search.session(entityManager);
        var list = searchSession.search(TestEntity.class)
            .select(MyProjection.class)
            .where(f ->
                f.simpleQueryString().field("myEnum").matching("a"))
            .fetchAll();

        log.info("{}", list);

    }

    @Test
    public void instantField() {
        var searchSession = Search.session(entityManager);
        var list = searchSession.search(TestEntity.class)
            .select(MyProjection.class)
            .where(f -> {
                return f.match().field("instant")
                    .matching(clock.instant());

            })

            .fetchAll();

        log.info("{}", list);
    }


    @Test
    public void size() {
        var searchSession = Search.session(entityManager);
        var list = searchSession.search(TestEntity.class)
            .select(MyProjection.class)
            .where(f -> {
                return f.match().field("listSize")
                    .matching(3);
            })

            .fetchAll();

        assertThat(list.hits()).hasSize(1);
        log.info("{}", list);
    }




}
