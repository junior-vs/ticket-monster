package br.vsjr.labs.ticketmonster.catalog.adapter.in.rest.dto;

import java.time.Instant;
import java.util.UUID;

import br.vsjr.labs.ticketmonster.catalog.domain.model.EventCategory;

/**
 * Representacao de saida de uma categoria de evento.
 *
 * <p>Contrato: schema {@code CategoryResponse} de
 * {@code contracts/event-categories-api.yaml} ({@code id}, {@code description},
 * {@code createdAt}).
 *
 * @param id          identificador UUID v4 da categoria
 * @param description descricao normalizada
 * @param createdAt   timestamp de criacao
 */
public record CategoryResponse(UUID id, String description, Instant createdAt) {

    /**
     * Projeta o agregado de dominio para a representacao de transporte.
     *
     * @param category categoria de dominio
     * @return DTO de resposta
     */
    public static CategoryResponse from(EventCategory category) {
        return new CategoryResponse(category.id(), category.description().value(), category.createdAt());
    }
}
