package br.vsjr.labs.ticketmonster.catalog.domain.exception;

/**
 * Lancada quando a descricao informada para uma categoria viola as invariantes
 * de dominio (nula, vazia ou acima do limite de caracteres).
 *
 * <p>FR-002 / FR-003.
 */
public class EventCategoryInvalidDescriptionException extends RuntimeException {

    public EventCategoryInvalidDescriptionException(String message) {
        super(message);
    }
}
