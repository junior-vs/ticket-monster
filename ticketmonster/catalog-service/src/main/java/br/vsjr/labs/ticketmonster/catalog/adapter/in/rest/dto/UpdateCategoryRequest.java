package br.vsjr.labs.ticketmonster.catalog.adapter.in.rest.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Corpo da requisicao de alteracao da descricao de uma categoria de evento.
 *
 * <p>Contrato: schema {@code UpdateCategoryRequest} de
 * {@code contracts/event-categories-api.yaml} ({@code description} obrigatoria,
 * entre 1 e 120 caracteres).
 *
 * <p>A validacao de dominio (obrigatoriedade, limite e normalizacao) e aplicada
 * pelo Value Object {@code EventCategoryDescription}, garantindo RFC 7807 na
 * resposta (FR-005).
 *
 * @param description nova descricao bruta informada pelo administrador
 */
public record UpdateCategoryRequest(@NotNull(message = "A descrição da categoria é obrigatória.") String description) {
}
