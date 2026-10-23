package br.vsjr.labs.ticketmonster.catalog.domain.model;

import br.vsjr.labs.ticketmonster.catalog.domain.exception.EventCategoryInvalidDescriptionException;

/**
 * Value Object imutavel que representa a descricao de uma categoria de evento.
 *
 * <p>Invariantes (RN06 / FR-002 / FR-002a / FR-003): a descricao nao pode ser
 * nula, vazia ou composta apenas por espacos, e deve ter no maximo
 * {@value #MAX_LENGTH} caracteres apos a normalizacao ({@code trim}).
 *
 * <p>Nucleo puro de dominio: nao depende de Quarkus, JPA, Redis ou Kafka.
 */
public record EventCategoryDescription(String value) {

    /** Limite maximo de caracteres da descricao (DDL {@code VARCHAR(120)}). */
    public static final int MAX_LENGTH = 120;

    public EventCategoryDescription {
        if (value == null) {
            throw new EventCategoryInvalidDescriptionException("A descrição da categoria é obrigatória.");
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new EventCategoryInvalidDescriptionException(
                    "A descrição da categoria não pode ser vazia ou composta apenas por espaços.");
        }
        if (normalized.length() > MAX_LENGTH) {
            throw new EventCategoryInvalidDescriptionException(
                    "A descrição da categoria deve ter no máximo " + MAX_LENGTH + " caracteres.");
        }
        value = normalized;
    }

    /**
     * Cria uma descricao normalizada a partir de um texto bruto.
     *
     * @param raw texto informado pelo usuario
     * @return descricao normalizada e validada
     */
    public static EventCategoryDescription of(String raw) {
        return new EventCategoryDescription(raw);
    }
}
