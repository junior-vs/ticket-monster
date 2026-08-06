package br.vsjr.labs.ticketmonster.catalog.integration;

import br.vsjr.labs.ticketmonster.catalog.adapter.out.repository.EventCategoryRepository;
import br.vsjr.labs.ticketmonster.catalog.domain.model.EventCategory;
import br.vsjr.labs.ticketmonster.catalog.domain.model.EventCategoryDescription;
import br.vsjr.labs.ticketmonster.catalog.domain.model.EventCategoryId;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class EventCategoryRepositoryTest {

    @Inject
    EventCategoryRepository repository;

    @Inject
    PgPool pgPool;

    @Test
    @RunOnVertxContext
    @DisplayName("US3 - should delete category when no events are associated")
    void shouldDeleteCategoryWhenNoEventsAreAssociated(UniAsserter asserter) {
        var description = "Category " + UUID.randomUUID();
        asserter.execute(() -> Panache.withTransaction(() -> repository.save(new EventCategory(new EventCategoryDescription(description))))
                .invoke(saved -> asserter.putData("categoryId", saved.id())));
        asserter.assertTrue(() -> Panache.withTransaction(() -> repository.deleteById((EventCategoryId) asserter.getData("categoryId"))));
    }

    @Test
    @RunOnVertxContext
    @DisplayName("US3/RN04 - should enforce ON DELETE RESTRICT when category has linked event")
    void shouldEnforceOnDeleteRestrictWhenCategoryHasLinkedEvent(UniAsserter asserter) {
        var description = "Linked " + UUID.randomUUID();
        var eventId = UUID.randomUUID();
        var eventName = "Event-" + UUID.randomUUID().toString().substring(0, 8);
        var eventDescription = "Descrição válida para evento associado na validação de delete.";

        asserter.execute(() -> Panache.withTransaction(() -> repository.save(new EventCategory(new EventCategoryDescription(description))))
                .invoke(saved -> asserter.putData("linkedCategoryId", saved.id())));
        asserter.execute(() -> {
            var categoryId = (br.vsjr.labs.ticketmonster.catalog.domain.model.EventCategoryId) asserter.getData("linkedCategoryId");
            return pgPool.preparedQuery("""
                INSERT INTO catalog.event (id, name, description, event_category_id, status, created_at, updated_at)
                VALUES ($1, $2, $3, $4, 'DRAFT', now(), now())
                """).execute(Tuple.of(eventId, eventName, eventDescription, categoryId.value())).replaceWithVoid();
        });

        asserter.assertFailedWith(
                () -> Panache.withTransaction(() -> repository.deleteById((EventCategoryId) asserter.getData("linkedCategoryId"))),
                RuntimeException.class);
        asserter.assertThat(
                () -> Panache.withSession(() -> repository.findById((EventCategoryId) asserter.getData("linkedCategoryId"))),
                result -> assertTrue(result.isPresent()));
    }
}
