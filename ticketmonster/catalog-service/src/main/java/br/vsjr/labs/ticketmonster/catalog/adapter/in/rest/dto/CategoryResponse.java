package br.vsjr.labs.ticketmonster.catalog.adapter.in.rest.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Representacao de saida de uma categoria de evento.
 *
 * <p>Contrato: schema {@code CategoryResponse} de
 * {@code contracts/event-categories-api.yaml} ({@code id}, {@code description},
 * {@code createdAt}).
 *
 * <p>A projecao a partir do agregado de dominio e feita por
 * {@code EventCategoryMapper}.
 *
 * @param id          identificador UUID v4 da categoria
 * @param description descricao normalizada
 * @param createdAt   timestamp de criacao
 */
public record CategoryResponse(UUID id, String description, Instant createdAt) {
}
