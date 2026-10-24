package br.vsjr.labs.ticketmonster.catalog.rest;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.vsjr.labs.ticketmonster.catalog.application.port.out.EventCategoryCachePort;
import br.vsjr.labs.ticketmonster.catalog.domain.model.EventCategory;

/**
 * Testes de contrato e do Cache-Aside Redis da listagem publica de categorias
 * (US-CAT-09 / P4).
 *
 * <p>FR-007 - o primeiro GET carrega do PostgreSQL e popula a chave
 * {@code catalog:categories:list}; os GETs seguintes sao servidos pelo Redis.
 * FR-007a - toda escrita invalida essa chave.
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
@QuarkusTestResource(RedisTestResource.class)
@TestSecurity(user = "admin", roles = "ROLE_ADMIN")
class EventCategoryResourceCacheTest {

    private static final String ADMIN_PATH = "/api/v1/admin/event-categories";
    private static final String PUBLIC_PATH = "/api/v1/event-categories";

    @Inject
    EventCategoryCachePort cachePort;

    @Inject
    DataSource dataSource;

    @Test
    @DisplayName("US4 / FR-001 - GET publico responde 200 com lista ordenada por descricao")
    void shouldListCategoriesOrderedByDescription() {
        String suffix = uniqueSuffix();
        String first = "AAA-" + suffix;
        String last = "ZZZ-" + suffix;
        createCategory(last);
        createCategory(first);

        List<String> descriptions = listDescriptions();

        int firstIndex = descriptions.indexOf(first);
        int lastIndex = descriptions.indexOf(last);
        assertTrue(firstIndex >= 0, "a categoria criada deve constar na listagem: " + first);
        assertTrue(lastIndex >= 0, "a categoria criada deve constar na listagem: " + last);
        assertTrue(firstIndex < lastIndex, "a listagem deve estar ordenada por descricao ascendente");
    }

    @Test
    @DisplayName("US4 / FR-007 - GET popula o cache Redis apos carregar do PostgreSQL")
    void shouldPopulateCacheOnRead() {
        String description = "Cache " + uniqueSuffix();
        createCategory(description);

        List<String> descriptions = listDescriptions();
        assertTrue(descriptions.contains(description));

        Optional<List<EventCategory>> cached = cachedCategories();
        assertTrue(cached.isPresent(), "o cache da listagem deve estar populado apos o GET");
        assertTrue(cached.get().stream()
                        .anyMatch(category -> description.equals(category.description().value())),
                "o cache deve conter a categoria lida");
    }

    @Test
    @DisplayName("US4 / FR-007a - escrita invalida o cache e o GET seguinte o repopula")
    void shouldInvalidateCacheOnWrite() {
        listDescriptions();
        assertTrue(cachedCategories().isPresent(), "pre-condicao: cache populado pelo GET");

        String description = "Invalidacao " + uniqueSuffix();
        createCategory(description);

        assertTrue(cachedCategories().isEmpty(), "a escrita deve invalidar a chave do cache");

        List<String> descriptions = listDescriptions();
        assertTrue(descriptions.contains(description), "o GET seguinte deve repopular o cache a partir da base");
        assertTrue(cachedCategories().isPresent());
    }

    @Test
    @DisplayName("US4 / FR-007 - GETs subsequentes sao servidos do cache (Cache-Aside)")
    void shouldServeSubsequentReadsFromCache() {
        listDescriptions();

        String insertedDirectly = "Direto " + uniqueSuffix();
        insertCategoryDirectly(insertedDirectly);

        List<String> descriptions = listDescriptions();

        assertTrue(cachedCategories().isPresent(), "o cache deve continuar sendo a fonte das leituras");
        assertFalse(descriptions.contains(insertedDirectly),
                "a listagem deve vir do cache e nao refletir alteracoes feitas fora do caso de uso");
    }

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private List<String> listDescriptions() {
        return given()
                .when().get(PUBLIC_PATH)
                .then()
                .statusCode(200)
                .extract().jsonPath().getList("description", String.class);
    }

    private Optional<List<EventCategory>> cachedCategories() {
        return cachePort.getCategoryList().await().indefinitely();
    }

    private void createCategory(String description) {
        given()
                .contentType(ContentType.JSON)
                .body("{\"description\": \"" + description + "\"}")
                .when().post(ADMIN_PATH)
                .then()
                .statusCode(201);
    }

    /**
     * Insere uma categoria diretamente no PostgreSQL, sem passar pelo caso de uso,
     * para provar que a listagem prioriza o cache.
     */
    private void insertCategoryDirectly(String description) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO catalog.event_category (description) VALUES (?)")) {
            statement.setString(1, description);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao inserir categoria diretamente no PostgreSQL", e);
        }
    }
}
