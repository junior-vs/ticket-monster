package br.vsjr.labs.ticketmonster.catalog.rest;

import java.util.HashMap;
import java.util.Map;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * Sobe um Redis descartavel via Testcontainers e aponta o cliente reativo para o
 * container.
 *
 * <p>AGENTS.md secao 10 - testes de integracao usam Testcontainers; jamais
 * mockar Postgres/Redis. Nao existe artefato {@code testcontainers-redis} nas
 * dependencias do modulo, portanto o container e criado a partir do
 * {@link GenericContainer}.
 */
public class RedisTestResource implements QuarkusTestResourceLifecycleManager {

    private static final String REDIS_IMAGE = "redis:7-alpine";
    private static final int REDIS_PORT = 6379;

    private GenericContainer<?> redis;

    @Override
    public Map<String, String> start() {
        redis = new GenericContainer<>(DockerImageName.parse(REDIS_IMAGE))
                .withExposedPorts(REDIS_PORT);
        redis.start();

        Map<String, String> config = new HashMap<>();
        config.put("quarkus.redis.hosts",
                "redis://" + redis.getHost() + ":" + redis.getMappedPort(REDIS_PORT));
        return config;
    }

    @Override
    public void stop() {
        if (redis != null) {
            redis.stop();
        }
    }
}
