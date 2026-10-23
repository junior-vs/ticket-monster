package br.vsjr.labs.ticketmonster.catalog.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Agregado raiz que representa uma categoria de eventos do catalogo.
 *
 * <p>Regras de negocio associadas:
 * <ul>
 *   <li>RN06 / FR-002a - descricao unica e normalizada, garantida por
 *       {@link EventCategoryDescription}; a comparacao/verificacao de unicidade
 *       e responsabilidade do caso de uso.</li>
 *   <li>FR-003 - descricao limitada a {@value EventCategoryDescription#MAX_LENGTH}
 *       caracteres.</li>
 * </ul>
 *
 * <p>Nucleo puro de dominio: nao depende de Quarkus, JPA, Redis ou Kafka.
 */
public final class EventCategory {

    private final UUID id;
    private EventCategoryDescription description;
    private final Instant createdAt;

    private EventCategory(UUID id, EventCategoryDescription description, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.description = Objects.requireNonNull(description, "description");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    /**
     * Cria uma nova categoria, gerando um identificador {@code UUID v4} e o
     * timestamp de criacao.
     *
     * @param description descricao ja normalizada e validada
     * @return nova instancia de {@link EventCategory}
     */
    public static EventCategory create(EventCategoryDescription description) {
        return new EventCategory(UUID.randomUUID(), description, Instant.now());
    }

    /**
     * Reconstitui uma categoria a partir do seu estado persistido, sem gerar
     * novo identificador.
     *
     * @param id identificador persistido
     * @param description descricao persistida
     * @param createdAt timestamp de criacao persistido
     * @return instancia reconstituida
     */
    public static EventCategory reconstitute(UUID id, EventCategoryDescription description, Instant createdAt) {
        return new EventCategory(id, description, createdAt);
    }

    /**
     * Altera a descricao da categoria.
     *
     * @param newDescription nova descricao normalizada e validada
     */
    public void changeDescription(EventCategoryDescription newDescription) {
        this.description = Objects.requireNonNull(newDescription, "newDescription");
    }

    public UUID id() {
        return id;
    }

    public EventCategoryDescription description() {
        return description;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
