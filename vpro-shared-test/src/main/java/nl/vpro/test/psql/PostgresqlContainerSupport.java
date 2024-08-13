package nl.vpro.test.psql;

import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import jakarta.inject.Inject;

import org.flywaydb.core.Flyway;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Will set up a postgresql container bean for using spring, which can be injected in (spring based) tests like so:
 * <pre>
 * {@code
 * @ExtendWith(SpringExtension.class)
 * @ContextConfiguration(classes = {
 *     PostgresqlContainerSupport.class,
 * })
 * public class MyTest {
 * ...
 *
 * @Inject
 * private DataSoure dataSource;
 *
 * </pre>
 */
@Configuration
@Slf4j
public class PostgresqlContainerSupport {

    public static final String POSTGRESQL_IMAGE = "postgres:16-alpine";

    public static PostgreSQLContainer<?> newContainer() {
        return new PostgreSQLContainer<>(POSTGRESQL_IMAGE);
    }

    @Bean("psqlcontainer")
    public PostgreSQLContainer<?> getPostgresqlContainer() {
        var postgresDBContainer = newContainer();
            //.withStartupTimeout(Duration.ofSeconds(180L))
            ;
        postgresDBContainer.start();
        return postgresDBContainer;
    }

    @Bean("dataSource")
    @Inject
    public DataSource getDataSource(PostgreSQLContainer<?> postgresDBContainer) {
        PGSimpleDataSource source = new PGSimpleDataSource();
        source.setURL(postgresDBContainer.getJdbcUrl());
        source.setPassword(postgresDBContainer.getPassword());
        source.setUser(postgresDBContainer.getUsername());
        log.info("{} : {} ({})", postgresDBContainer.getUsername(), postgresDBContainer.getPassword(), postgresDBContainer.getJdbcUrl());

        try {
            Flyway flyway = Flyway.configure()
                .table("schema_version")
                .dataSource(source)
                .validateMigrationNaming(true)
                .load();
            flyway.migrate();
        } catch (NoClassDefFoundError t) {
            log.info(t.getMessage());
        }
        return source;

    }
}
