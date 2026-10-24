package br.vsjr.labs.ticketmonster.catalog.rest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

import javax.sql.DataSource;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import static io.restassured.RestAssured.given;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;

/**
 * Testes de integracao REST do gerenciamento de categorias de eventos (US-CAT-09).
 *
 * <p>Cobre os fluxos P1 (FR-006: criacao com 201), P2 (US2: alteracao com 200),
 * P3 (US3: exclusao com 204 e bloqueio por eventos vinculados com 409) e a
 * unicidade (RN06 / FR-002a) com 409, sempre no formato RFC 7807 (FR-005).
 * Infraestrutura real via Testcontainers PostgreSQL.
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
@TestSecurity(user = "admin", roles = "ROLE_ADMIN")
class EventCategoryResourceTest {

    private static final String BASE_PATH = "/api/v1/admin/event-categories";
    private static final String PUBLIC_PATH = "/api/v1/event-categories";
    private static final String PROBLEM_JSON = "application/problem+json";
    private static final String TYPE_CONFLICT = "https://ticketmonster.com/errors/conflict";
    private static final String TYPE_VALIDATION = "https://ticketmonster.com/errors/validation";
    private static final String TYPE_NOT_FOUND = "https://ticketmonster.com/errors/not-found";

    @Inject
    DataSource dataSource;

    @Test
    @DisplayName("P1 / FR-001 - POST cria categoria e responde 201 com UUID gerado")
    void shouldCreateCategory() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"description\": \"Orquestra\"}")
                .when().post(BASE_PATH)
                .then()
                .statusCode(201)
                .header("Location", containsString(BASE_PATH + "/"))
                .body("id", notNullValue())
                .body("description", equalTo("Orquestra"))
                .body("createdAt", notNullValue());
    }

    @Test
    @DisplayName("FR-002a - a descricao e persistida normalizada (trim)")
    void shouldNormalizeDescriptionOnCreate() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"description\": \"  Stand-up Comedy  \"}")
                .when().post(BASE_PATH)
                .then()
                .statusCode(201)
                .body("description", equalTo("Stand-up Comedy"));
    }

    @Test
    @DisplayName("RN06 - descricao duplicada responde 409 em RFC 7807")
    void shouldRejectDuplicateDescription() {
        createCategory("Rock");

        given()
                .contentType(ContentType.JSON)
                .body("{\"description\": \"Rock\"}")
                .when().post(BASE_PATH)
                .then()
                .statusCode(409)
                .contentType(PROBLEM_JSON)
                .body("type", equalTo(TYPE_CONFLICT))
                .body("title", equalTo("Categoria Duplicada"))
                .body("status", equalTo(409))
                .body("detail", equalTo("Já existe uma categoria cadastrada com a descrição 'Rock'."));
    }

    @Test
    @DisplayName("RN06 / FR-002a - duplicata apos normalizacao (trim) responde 409")
    void shouldRejectDuplicateAfterTrim() {
        createCategory("Teatro");

        given()
                .contentType(ContentType.JSON)
                .body("{\"description\": \"   Teatro   \"}")
                .when().post(BASE_PATH)
                .then()
                .statusCode(409)
                .contentType(PROBLEM_JSON)
                .body("type", equalTo(TYPE_CONFLICT))
                .body("detail", equalTo("Já existe uma categoria cadastrada com a descrição 'Teatro'."));
    }

    @Test
    @DisplayName("FR-002 / FR-005 - descricao em branco responde 400 em RFC 7807")
    void shouldRejectBlankDescription() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"description\": \"   \"}")
                .when().post(BASE_PATH)
                .then()
                .statusCode(400)
                .contentType(PROBLEM_JSON)
                .body("type", equalTo(TYPE_VALIDATION))
                .body("title", equalTo("Descrição Inválida"))
                .body("status", equalTo(400));
    }

    @Test
    @DisplayName("FR-005 - corpo sem 'description' responde 400 em RFC 7807 (Bean Validation)")
    void shouldRejectMissingDescription() {
        given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when().post(BASE_PATH)
                .then()
                .statusCode(400)
                .contentType(PROBLEM_JSON)
                .body("type", equalTo(TYPE_VALIDATION))
                .body("status", equalTo(400))
                .body("instance", equalTo(BASE_PATH));
    }

    @Test
    @DisplayName("FR-003 - descricao acima de 120 caracteres responde 400 em RFC 7807")
    void shouldRejectDescriptionAboveLimit() {
        String overLimit = "a".repeat(121);

        given()
                .contentType(ContentType.JSON)
                .body("{\"description\": \"" + overLimit + "\"}")
                .when().post(BASE_PATH)
                .then()
                .statusCode(400)
                .contentType(PROBLEM_JSON)
                .body("type", equalTo(TYPE_VALIDATION))
                .body("status", equalTo(400));
    }

    // ==========================================================
    // US2 - Alterar descricao da categoria (P2)
    // ==========================================================

    @Test
    @DisplayName("US2 / FR-001 - PUT altera a descricao e responde 200")
    void shouldUpdateCategory() {
        String id = createCategoryReturningId("Cinema");

        given()
                .contentType(ContentType.JSON)
                .body("{\"description\": \"Cinema Nacional\"}")
                .when().put(BASE_PATH + "/" + id)
                .then()
                .statusCode(200)
                .body("id", equalTo(id))
                .body("description", equalTo("Cinema Nacional"))
                .body("createdAt", notNullValue());
    }

    @Test
    @DisplayName("US2 / FR-002a - PUT normaliza a descricao (trim)")
    void shouldNormalizeDescriptionOnUpdate() {
        String id = createCategoryReturningId("Circo");

        given()
                .contentType(ContentType.JSON)
                .body("{\"description\": \"  Circo Contemporâneo  \"}")
                .when().put(BASE_PATH + "/" + id)
                .then()
                .statusCode(200)
                .body("description", equalTo("Circo Contemporâneo"));
    }

    @Test
    @DisplayName("US2 / RN06 - PUT com a propria descricao normalizada e aceito (200)")
    void shouldAllowUpdateKeepingSameDescription() {
        String id = createCategoryReturningId("Ópera");

        given()
                .contentType(ContentType.JSON)
                .body("{\"description\": \"  Ópera  \"}")
                .when().put(BASE_PATH + "/" + id)
                .then()
                .statusCode(200)
                .body("description", equalTo("Ópera"));
    }

    @Test
    @DisplayName("US2 / RN06 - PUT com descricao de outra categoria responde 409 em RFC 7807")
    void shouldRejectDuplicateDescriptionOnUpdate() {
        createCategory("Jazz");
        String id = createCategoryReturningId("Blues");

        given()
                .contentType(ContentType.JSON)
                .body("{\"description\": \"Jazz\"}")
                .when().put(BASE_PATH + "/" + id)
                .then()
                .statusCode(409)
                .contentType(PROBLEM_JSON)
                .body("type", equalTo(TYPE_CONFLICT))
                .body("title", equalTo("Categoria Duplicada"))
                .body("status", equalTo(409))
                .body("detail", equalTo("Já existe uma categoria cadastrada com a descrição 'Jazz'."));
    }

    @Test
    @DisplayName("US2 / FR-005 - PUT em id inexistente responde 404 em RFC 7807")
    void shouldReturnNotFoundOnUpdate() {
        UUID unknownId = UUID.randomUUID();

        given()
                .contentType(ContentType.JSON)
                .body("{\"description\": \"Qualquer\"}")
                .when().put(BASE_PATH + "/" + unknownId)
                .then()
                .statusCode(404)
                .contentType(PROBLEM_JSON)
                .body("type", equalTo(TYPE_NOT_FOUND))
                .body("title", equalTo("Categoria Não Encontrada"))
                .body("status", equalTo(404))
                .body("detail", equalTo("Categoria de evento não encontrada para o id " + unknownId + "."));
    }

    @Test
    @DisplayName("US2 / FR-002 - PUT com descricao em branco responde 400 em RFC 7807")
    void shouldRejectBlankDescriptionOnUpdate() {
        String id = createCategoryReturningId("Teatro Infantil");

        given()
                .contentType(ContentType.JSON)
                .body("{\"description\": \"   \"}")
                .when().put(BASE_PATH + "/" + id)
                .then()
                .statusCode(400)
                .contentType(PROBLEM_JSON)
                .body("type", equalTo(TYPE_VALIDATION))
                .body("status", equalTo(400));
    }

    @Test
    @DisplayName("US2 / FR-003 - PUT com descricao acima de 120 caracteres responde 400")
    void shouldRejectOverLimitDescriptionOnUpdate() {
        String id = createCategoryReturningId("Dança");
        String overLimit = "a".repeat(121);

        given()
                .contentType(ContentType.JSON)
                .body("{\"description\": \"" + overLimit + "\"}")
                .when().put(BASE_PATH + "/" + id)
                .then()
                .statusCode(400)
                .contentType(PROBLEM_JSON)
                .body("type", equalTo(TYPE_VALIDATION))
                .body("status", equalTo(400));
    }

    // ==========================================================
    // US3 - Excluir categoria (P3)
    // ==========================================================

    @Test
    @DisplayName("US3 / FR-004 - DELETE de categoria sem eventos responde 204 e remove da listagem")
    void shouldDeleteCategoryWithoutEvents() {
        String id = createCategoryReturningId("Categoria Descartavel");

        given()
                .when().delete(BASE_PATH + "/" + id)
                .then()
                .statusCode(204);

        given()
                .when().get(PUBLIC_PATH)
                .then()
                .statusCode(200)
                .body("description", not(hasItem("Categoria Descartavel")));
    }

    @Test
    @DisplayName("US3 / FR-005 - DELETE de id inexistente responde 404 em RFC 7807")
    void shouldReturnNotFoundOnDelete() {
        UUID unknownId = UUID.randomUUID();

        given()
                .when().delete(BASE_PATH + "/" + unknownId)
                .then()
                .statusCode(404)
                .contentType(PROBLEM_JSON)
                .body("type", equalTo(TYPE_NOT_FOUND))
                .body("title", equalTo("Categoria Não Encontrada"))
                .body("status", equalTo(404));
    }

    @Test
    @DisplayName("US3 / RN04 - DELETE de categoria com evento vinculado responde 409 em RFC 7807")
    void shouldRejectDeleteWhenCategoryHasEvents() {
        String id = createCategoryReturningId("Festival");
        linkEventToCategory(UUID.fromString(id));

        given()
                .when().delete(BASE_PATH + "/" + id)
                .then()
                .statusCode(409)
                .contentType(PROBLEM_JSON)
                .body("type", equalTo(TYPE_CONFLICT))
                .body("title", equalTo("Restrição de Integridade Referencial"))
                .body("status", equalTo(409))
                .body("detail", equalTo("Não é possível excluir a categoria pois existem eventos associados a ela."))
                .body("instance", equalTo(BASE_PATH + "/" + id));
    }

    private void createCategory(String description) {
        createCategoryReturningId(description);
    }

    private String createCategoryReturningId(String description) {
        return given()
                .contentType(ContentType.JSON)
                .body("{\"description\": \"" + description + "\"}")
                .when().post(BASE_PATH)
                .then()
                .statusCode(201)
                .extract().jsonPath().getString("id");
    }

    /**
     * Insere diretamente um evento vinculado a categoria, exercitando a
     * integridade referencial criada por {@code V1.0.2__event_reference_restrict.sql}.
     */
    private void linkEventToCategory(UUID categoryId) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO catalog.event (event_category_id) VALUES (?)")) {
            statement.setObject(1, categoryId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao vincular evento de teste a categoria " + categoryId, e);
        }
    }
}
