package br.vsjr.labs.ticketmonster.catalog.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
class EventCategoryResourceSecurityTest {

    @Test
    @DisplayName("US3 - should return 401 when deleting category without authentication")
    void shouldReturnUnauthorizedWhenDeletingWithoutAuthentication() {
        given()
                .when()
                .delete("/api/v1/admin/event-categories/77777777-7777-7777-7777-777777777777")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "customer-user", roles = "ROLE_CUSTOMER")
    @DisplayName("US3 - should return 403 when user role is not admin")
    void shouldReturnForbiddenWhenDeletingWithoutAdminRole() {
        given()
                .when()
                .delete("/api/v1/admin/event-categories/88888888-8888-8888-8888-888888888888")
                .then()
                .statusCode(403);
    }
}
