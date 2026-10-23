package br.vsjr.labs.ticketmonster.catalog.rest;

import java.util.HashMap;
import java.util.Map;

import org.testcontainers.postgresql.PostgreSQLContainer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * Sobe um PostgreSQL descartavel via Testcontainers e redireciona o datasource
 * (JDBC para o Liquibase e reativo para o Hibernate/Panache) para o container.
 *
 * <p>AGENTS.md secao 10 - testes de integracao usam Testcontainers; jamais
 * mockar Postgres/Redis.
 */
public class PostgresTestResource implements QuarkusTestResourceLifecycleManager {

    private static final String POSTGRES_IMAGE = "postgres:16-alpine";

    private PostgreSQLContainer postgres;

    @Override
    public Map<String, String> start() {
        postgres = new PostgreSQLContainer(POSTGRES_IMAGE);
        postgres.start();

        Map<String, String> config = new HashMap<>();
        config.put("quarkus.datasource.username", postgres.getUsername());
        config.put("quarkus.datasource.password", postgres.getPassword());
        config.put("quarkus.datasource.jdbc.url", postgres.getJdbcUrl());
        config.put("quarkus.datasource.reactive.url",
                "postgresql://" + postgres.getHost() + ":" + postgres.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT)
                        + "/" + postgres.getDatabaseName());
        return config;
    }

    @Override
    public void stop() {
        if (postgres != null) {
            postgres.stop();
        }
    }
}
