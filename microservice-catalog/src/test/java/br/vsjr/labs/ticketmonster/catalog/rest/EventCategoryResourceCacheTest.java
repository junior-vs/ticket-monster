package br.vsjr.labs.ticketmonster.catalog.rest;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class EventCategoryResourceCacheTest {

    private static final String CACHE_KEY = "catalog:categories:list";

    @Inject
    PgPool pgPool;

    @Inject
    RedisDataSource redisDataSource;

    @BeforeEach
    void setUp() {
        pgPool.query("DELETE FROM catalog.event").execute().await().indefinitely();
        pgPool.query("DELETE FROM catalog.event_category").execute().await().indefinitely();

        pgPool.preparedQuery("INSERT INTO catalog.event_category (id, description, created_at) VALUES ($1, $2, now())")
                .execute(Tuple.of(UUID.randomUUID(), "Festivals"))
                .await().indefinitely();
        pgPool.preparedQuery("INSERT INTO catalog.event_category (id, description, created_at) VALUES ($1, $2, now())")
                .execute(Tuple.of(UUID.randomUUID(), "Concerts"))
                .await().indefinitely();

        keys().del(CACHE_KEY);
    }

    @Test
    @DisplayName("US4 - should serve sorted list and cache results in Redis")
    void shouldServeSortedListAndCacheResultsInRedis() {
        given()
                .when()
                .get("/api/v1/event-categories")
                .then()
                .statusCode(200)
                .body("size()", is(2))
                .body("[0].description", is("Concerts"))
                .body("[1].description", is("Festivals"));

        var cachedPayload = values().get(CACHE_KEY);
        assertNotNull(cachedPayload);

        pgPool.query("DELETE FROM catalog.event").execute().await().indefinitely();
        pgPool.query("DELETE FROM catalog.event_category").execute().await().indefinitely();

        given()
                .when()
                .get("/api/v1/event-categories")
                .then()
                .statusCode(200)
                .body("size()", is(2))
                .body("[0].description", is("Concerts"))
                .body("[1].description", is("Festivals"));
    }

    private ValueCommands<String, String> values() {
        return redisDataSource.value(String.class);
    }

    private KeyCommands<String> keys() {
        return redisDataSource.key();
    }
}
