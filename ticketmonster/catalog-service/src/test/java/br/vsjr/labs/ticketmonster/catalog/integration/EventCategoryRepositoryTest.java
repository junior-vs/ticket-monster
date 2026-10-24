package br.vsjr.labs.ticketmonster.catalog.integration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.vsjr.labs.ticketmonster.catalog.application.port.out.EventCategoryRepositoryPort;
import br.vsjr.labs.ticketmonster.catalog.application.port.out.EventRepositoryPort;
import br.vsjr.labs.ticketmonster.catalog.domain.model.EventCategory;
import br.vsjr.labs.ticketmonster.catalog.domain.model.EventCategoryDescription;
import br.vsjr.labs.ticketmonster.catalog.rest.PostgresTestResource;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import jakarta.inject.Inject;

/**
 * Testes de integracao da persistencia de categorias de eventos (US-CAT-09 / P3).
 *
 * <p>Valida no banco real (Testcontainers PostgreSQL) que a exclusao de uma
 * categoria e protegida pela integridade referencial {@code ON DELETE RESTRICT}
 * (RN04 / FR-004), independentemente da verificacao feita no caso de uso.
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class EventCategoryRepositoryTest {

    private static final String SQL_INSERT_EVENT =
            "INSERT INTO catalog.event (event_category_id) VALUES (?)";
    private static final String SQL_DELETE_EVENTS =
            "DELETE FROM catalog.event WHERE event_category_id = ?";

    @Inject
    EventCategoryRepositoryPort repositoryPort;

    @Inject
    EventRepositoryPort eventRepositoryPort;

    @Inject
    DataSource dataSource;

    @Inject
    Vertx vertx;

    @Test
    @DisplayName("US3 / FR-004 - remove a categoria quando nao ha eventos vinculados")
    void shouldRemoveCategoryWithoutEvents() {
        UUID id = createCategory("Sem eventos " + UUID.randomUUID());

        Boolean removed = await(() -> Panache.withTransaction(() -> repositoryPort.removeById(id)));

        assertEquals(Boolean.TRUE, removed);
        assertTrue(findCategory(id).isEmpty(), "a categoria removida nao deve mais existir");
    }

    @Test
    @DisplayName("US3 / RN04 - ON DELETE RESTRICT bloqueia a remocao quando ha evento vinculado")
    void shouldBlockRemovalWhenCategoryHasEvents() {
        UUID id = createCategory("Protegida " + UUID.randomUUID());
        executeUpdate(SQL_INSERT_EVENT, id);

        assertTrue(await(() -> Panache.withSession(() -> eventRepositoryPort.existsByCategoryId(id))),
                "deve detectar evento vinculado a categoria");

        Throwable failure = assertThrows(Throwable.class,
                () -> await(() -> Panache.withTransaction(() -> repositoryPort.removeById(id))));

        assertForeignKeyViolation(failure);
        assertTrue(findCategory(id).isPresent(), "a categoria deve permanecer apos a violacao de FK");
    }

    @Test
    @DisplayName("US3 / RN04 - apos excluir os eventos a categoria pode ser removida")
    void shouldAllowRemovalAfterEventsDeleted() {
        UUID id = createCategory("Liberada " + UUID.randomUUID());
        executeUpdate(SQL_INSERT_EVENT, id);
        executeUpdate(SQL_DELETE_EVENTS, id);

        assertFalse(await(() -> Panache.withSession(() -> eventRepositoryPort.existsByCategoryId(id))),
                "nao deve haver evento vinculado apos a exclusao");

        assertEquals(Boolean.TRUE, await(() -> Panache.withTransaction(() -> repositoryPort.removeById(id))));
        assertTrue(findCategory(id).isEmpty());
    }

    private UUID createCategory(String description) {
        return await(() -> Panache.withTransaction(() -> repositoryPort
                .save(EventCategory.create(EventCategoryDescription.of(description)))))
                .id();
    }

    private Optional<EventCategory> findCategory(UUID id) {
        return await(() -> Panache.withSession(() -> repositoryPort.findByIdOrEmpty(id)));
    }

    /**
     * Executa a operacao reativa em um contexto Vert.x duplicado e explicitamente
     * marcado como seguro, requisitos do Hibernate Reactive/Panache quando o caso
     * de teste roda fora do event loop.
     *
     * <p>A operacao e criada apenas na subscricao, ja dentro do contexto, pois o
     * Panache resolve o contexto no momento da construcao do {@code Uni}.
     *
     * @param operation fabrica da operacao reativa
     * @param <T>       tipo do item emitido
     * @return item emitido
     */
    private <T> T await(Supplier<Uni<? extends T>> operation) {
        Context context = VertxContext.getOrCreateDuplicatedContext(vertx);
        VertxContextSafetyToggle.setContextSafe(context, true);
        Uni<T> uni = Uni.createFrom().deferred(operation);
        return uni.runSubscriptionOn(runnable -> context.runOnContext(ignored -> runnable.run()))
                .await().indefinitely();
    }

    private void executeUpdate(String sql, UUID categoryId) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, categoryId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao executar comando de preparacao no PostgreSQL", e);
        }
    }

    private static void assertForeignKeyViolation(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof SQLException sqlException && "23503".equals(sqlException.getSQLState())) {
                return;
            }
            String message = current.getMessage();
            if (message != null && message.toLowerCase().contains("foreign key constraint")) {
                return;
            }
            current = current.getCause();
        }
        throw new AssertionError(
                "Esperada violacao de integridade referencial (SQLState 23503), mas foi: " + failure, failure);
    }
}
