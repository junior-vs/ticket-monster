package br.vsjr.labs.ticketmonster.catalog.rest;

import static io.restassured.RestAssured.given;

import java.util.UUID;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testes de seguranca (RBAC) dos endpoints de categorias de eventos (US-CAT-09).
 *
 * <p>AGENTS.md secao 8 - a leitura publica deve ser anonima (200) e as escritas
 * administrativas exigem {@code ROLE_ADMIN}: sem token responde 401 e, com token
 * sem a role exigida, responde 403.
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class EventCategoryResourceSecurityTest {

    private static final String ADMIN_PATH = "/api/v1/admin/event-categories";
    private static final String PUBLIC_PATH = "/api/v1/event-categories";
    private static final String BODY = "{\"description\": \"Seguranca\"}";

    @Test
    @DisplayName("secao 8 - GET publico responde 200 sem token")
    void shouldAllowAnonymousRead() {
        given()
                .when().get(PUBLIC_PATH)
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("secao 8 - POST sem token responde 401")
    void shouldRejectAnonymousCreate() {
        given()
                .contentType(ContentType.JSON)
                .body(BODY)
                .when().post(ADMIN_PATH)
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("secao 8 - PUT sem token responde 401")
    void shouldRejectAnonymousUpdate() {
        given()
                .contentType(ContentType.JSON)
                .body(BODY)
                .when().put(ADMIN_PATH + "/" + UUID.randomUUID())
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("secao 8 - DELETE sem token responde 401")
    void shouldRejectAnonymousDelete() {
        given()
                .when().delete(ADMIN_PATH + "/" + UUID.randomUUID())
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "customer", roles = "ROLE_CUSTOMER")
    @DisplayName("secao 8 - POST com ROLE_CUSTOMER responde 403")
    void shouldForbidCreateWithoutAdminRole() {
        given()
                .contentType(ContentType.JSON)
                .body(BODY)
                .when().post(ADMIN_PATH)
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "customer", roles = "ROLE_CUSTOMER")
    @DisplayName("secao 8 - PUT com ROLE_CUSTOMER responde 403")
    void shouldForbidUpdateWithoutAdminRole() {
        given()
                .contentType(ContentType.JSON)
                .body(BODY)
                .when().put(ADMIN_PATH + "/" + UUID.randomUUID())
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "customer", roles = "ROLE_CUSTOMER")
    @DisplayName("secao 8 - DELETE com ROLE_CUSTOMER responde 403")
    void shouldForbidDeleteWithoutAdminRole() {
        given()
                .when().delete(ADMIN_PATH + "/" + UUID.randomUUID())
                .then()
                .statusCode(403);
    }
}
