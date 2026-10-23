package br.vsjr.labs.ticketmonster.catalog.domain.exception;

/**
 * Lancada quando ja existe uma categoria com a mesma descricao normalizada.
 *
 * <p>RN06 / FR-002a - a unicidade considera a descricao apos normalizacao
 * ({@code trim}) das extremidades.
 */
public class EventCategoryAlreadyExistsException extends RuntimeException {

    public EventCategoryAlreadyExistsException(String description) {
        super("Já existe uma categoria cadastrada com a descrição '" + description + "'.");
    }
}
