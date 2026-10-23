package br.vsjr.labs.ticketmonster.catalog.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testes de integracao REST do cadastro de categorias de eventos (US-CAT-09 / P1).
 *
 * <p>Cobre o fluxo P1 (FR-006): criacao com 201, unicidade (RN06 / FR-002a) com
 * 409 e validacao de descricao invalida com 400, sempre no formato RFC 7807
 * (FR-005). Infraestrutura real via Testcontainers PostgreSQL.
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
@TestSecurity(user = "admin", roles = "ROLE_ADMIN")
class EventCategoryResourceTest {

    private static final String BASE_PATH = "/api/v1/admin/event-categories";
    private static final String PROBLEM_JSON = "application/problem+json";
    private static final String TYPE_CONFLICT = "https://ticketmonster.com/errors/conflict";
    private static final String TYPE_VALIDATION = "https://ticketmonster.com/errors/validation";

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

    private void createCategory(String description) {
        given()
                .contentType(ContentType.JSON)
                .body("{\"description\": \"" + description + "\"}")
                .when().post(BASE_PATH)
                .then()
                .statusCode(201);
    }
}
