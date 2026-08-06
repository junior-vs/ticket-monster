package br.vsjr.labs.ticketmonster.catalog.rest;

import br.vsjr.labs.ticketmonster.catalog.application.port.in.CreateCategoryUseCase;
import br.vsjr.labs.ticketmonster.catalog.application.port.in.DeleteCategoryUseCase;
import br.vsjr.labs.ticketmonster.catalog.application.port.in.ListCategoriesUseCase;
import br.vsjr.labs.ticketmonster.catalog.application.port.in.UpdateCategoryUseCase;
import br.vsjr.labs.ticketmonster.catalog.domain.exception.EventCategoryAlreadyExistsException;
import br.vsjr.labs.ticketmonster.catalog.domain.exception.EventCategoryInUseException;
import br.vsjr.labs.ticketmonster.catalog.domain.exception.EventCategoryNotFoundException;
import br.vsjr.labs.ticketmonster.catalog.domain.model.EventCategory;
import br.vsjr.labs.ticketmonster.catalog.domain.model.EventCategoryDescription;
import br.vsjr.labs.ticketmonster.catalog.domain.model.EventCategoryId;
import br.vsjr.labs.ticketmonster.catalog.domain.vo.PageRequest;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestSecurity(user = "admin-user", roles = "ROLE_ADMIN")
class EventCategoryResourceTest {

    @InjectMock
    CreateCategoryUseCase createCategoryUseCase;

    @InjectMock
    UpdateCategoryUseCase updateCategoryUseCase;

    @InjectMock
    DeleteCategoryUseCase deleteCategoryUseCase;

    @InjectMock
    ListCategoriesUseCase listCategoriesUseCase;

    @Test
    @DisplayName("US1/RN06/FR-002a - should create category with 201 and location")
    void shouldCreateCategory() {
        var created = new EventCategory(
                new EventCategoryId(UUID.fromString("11111111-1111-1111-1111-111111111111")),
                new EventCategoryDescription("Shows"),
                Instant.parse("2026-01-01T00:00:00Z"));

        when(createCategoryUseCase.createCategory(any(EventCategory.class)))
                .thenReturn(Uni.createFrom().item(created));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"description":"Shows"}
                        """)
                .when()
                .post("/api/v1/admin/event-categories")
                .then()
                .statusCode(201)
                .header("Location", endsWith("/api/v1/admin/event-categories/11111111-1111-1111-1111-111111111111"))
                .body("description", is("Shows"));
    }

    @Test
    @DisplayName("US1/RN06 - should return 409 problem details when category already exists")
    void shouldReturnConflictForDuplicateCategory() {
        when(createCategoryUseCase.createCategory(any(EventCategory.class)))
                .thenReturn(Uni.createFrom().failure(new EventCategoryAlreadyExistsException("Duplicada")));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"description":"Shows"}
                        """)
                .when()
                .post("/api/v1/admin/event-categories")
                .then()
                .statusCode(409)
                .contentType("application/problem+json")
                .body("status", is(409))
                .body("type", is("https://ticketmonster.com/errors/conflict"));
    }

    @Test
    @DisplayName("US1 - should return 400 for invalid payload")
    void shouldReturnBadRequestForInvalidPayload() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"description":"   "}
                        """)
                .when()
                .post("/api/v1/admin/event-categories")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("US2 - should update category with 200")
    void shouldUpdateCategory() {
        var updated = new EventCategory(
                new EventCategoryId(UUID.fromString("22222222-2222-2222-2222-222222222222")),
                new EventCategoryDescription("Festival"),
                Instant.parse("2026-01-01T00:00:00Z"));

        when(updateCategoryUseCase.updateCategory(any(EventCategoryId.class), any(EventCategoryDescription.class)))
                .thenReturn(Uni.createFrom().item(updated));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"description":"Festival"}
                        """)
                .when()
                .put("/api/v1/admin/event-categories/22222222-2222-2222-2222-222222222222")
                .then()
                .statusCode(200)
                .body("description", is("Festival"));
    }

    @Test
    @DisplayName("US2 - should return 409 on duplicated description")
    void shouldReturnConflictOnUpdateDuplicateDescription() {
        when(updateCategoryUseCase.updateCategory(any(EventCategoryId.class), any(EventCategoryDescription.class)))
                .thenReturn(Uni.createFrom().failure(new EventCategoryAlreadyExistsException("Duplicada")));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"description":"Festival"}
                        """)
                .when()
                .put("/api/v1/admin/event-categories/22222222-2222-2222-2222-222222222222")
                .then()
                .statusCode(409)
                .contentType("application/problem+json")
                .body("status", is(409))
                .body("type", is("https://ticketmonster.com/errors/conflict"));
    }

    @Test
    @DisplayName("US2 - should return 404 when category does not exist")
    void shouldReturnNotFoundOnUpdateMissingCategory() {
        when(updateCategoryUseCase.updateCategory(any(EventCategoryId.class), any(EventCategoryDescription.class)))
                .thenReturn(Uni.createFrom().failure(new EventCategoryNotFoundException("Nao encontrada")));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"description":"Festival"}
                        """)
                .when()
                .put("/api/v1/admin/event-categories/33333333-3333-3333-3333-333333333333")
                .then()
                .statusCode(404)
                .contentType("application/problem+json")
                .body("status", is(404))
                .body("type", is("https://ticketmonster.com/errors/not-found"));
    }

    @Test
    @DisplayName("US3 - should delete category with 204 when no events are associated")
    void shouldDeleteCategoryWhenNoEventsAreAssociated() {
        when(deleteCategoryUseCase.deleteCategory(any(EventCategoryId.class)))
                .thenReturn(Uni.createFrom().voidItem());

        given()
                .when()
                .delete("/api/v1/admin/event-categories/44444444-4444-4444-4444-444444444444")
                .then()
                .statusCode(204);
    }

    @Test
    @DisplayName("US3 - should return 409 problem details when category is in use")
    void shouldReturnConflictWhenCategoryIsInUse() {
        when(deleteCategoryUseCase.deleteCategory(any(EventCategoryId.class)))
                .thenReturn(Uni.createFrom().failure(new EventCategoryInUseException("44444444-4444-4444-4444-444444444444")));

        given()
                .when()
                .delete("/api/v1/admin/event-categories/44444444-4444-4444-4444-444444444444")
                .then()
                .statusCode(409)
                .contentType("application/problem+json")
                .body("status", is(409))
                .body("type", is("https://ticketmonster.com/errors/conflict"));
    }

    @Test
    @DisplayName("US3 - should return 404 problem details when category does not exist")
    void shouldReturnNotFoundWhenDeletingMissingCategory() {
        when(deleteCategoryUseCase.deleteCategory(any(EventCategoryId.class)))
                .thenReturn(Uni.createFrom().failure(new EventCategoryNotFoundException("Nao encontrada")));

        given()
                .when()
                .delete("/api/v1/admin/event-categories/99999999-9999-9999-9999-999999999999")
                .then()
                .statusCode(404)
                .contentType("application/problem+json")
                .body("status", is(404))
                .body("type", is("https://ticketmonster.com/errors/not-found"));
    }

    @Test
    @DisplayName("US4 - should list categories publicly")
    void shouldListCategoriesPublicly() {
        var first = new EventCategory(
                new EventCategoryId(UUID.fromString("55555555-5555-5555-5555-555555555555")),
                new EventCategoryDescription("Concerts"),
                Instant.parse("2026-01-01T00:00:00Z"));
        var second = new EventCategory(
                new EventCategoryId(UUID.fromString("66666666-6666-6666-6666-666666666666")),
                new EventCategoryDescription("Festivals"),
                Instant.parse("2026-01-02T00:00:00Z"));

        when(listCategoriesUseCase.listAllCategories(any(PageRequest.class)))
                .thenReturn(Uni.createFrom().item(List.of(first, second)));

        given()
                .when()
                .get("/api/v1/event-categories")
                .then()
                .statusCode(200)
                .body("size()", is(2))
                .body("[0].description", is("Concerts"))
                .body("[1].description", is("Festivals"));
    }
}
