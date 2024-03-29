package nl.vpro.test.psql;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;
import jakarta.inject.Inject;

@Configuration
@Slf4j
public class PostgresqlContainerSupport {

    @Bean("psqlcontainer")
    public PostgreSQLContainer<?> getPostgresqlContainer() {
        PostgreSQLContainer<?> postgresDBContainer = new PostgreSQLContainer<>("postgres:13")
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


        Flyway flyway = Flyway.configure()
            .table("schema_version")
            .dataSource(source)
            .validateMigrationNaming(true)
            .load();
        flyway.migrate();
        return source;

    }
}
