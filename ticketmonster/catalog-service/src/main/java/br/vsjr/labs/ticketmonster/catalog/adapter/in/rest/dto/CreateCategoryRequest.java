package br.vsjr.labs.ticketmonster.catalog.adapter.in.rest.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Corpo da requisicao de cadastro de uma categoria de evento.
 *
 * <p>Contrato: schema {@code CreateCategoryRequest} de
 * {@code contracts/event-categories-api.yaml} ({@code description} obrigatoria,
 * entre 1 e 120 caracteres).
 *
 * <p>A validacao de dominio (obrigatoriedade, limite e normalizacao) e aplicada
 * pelo Value Object {@code EventCategoryDescription}, garantindo RFC 7807 na
 * resposta (FR-005).
 *
 * @param description descricao bruta informada pelo administrador
 */
public record CreateCategoryRequest(@NotNull(message = "A descrição da categoria é obrigatória.") String description) {
}
