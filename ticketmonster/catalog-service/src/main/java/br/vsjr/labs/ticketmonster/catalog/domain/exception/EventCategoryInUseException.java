package br.vsjr.labs.ticketmonster.catalog.domain.exception;

/**
 * Lancada ao tentar excluir uma categoria que possui eventos associados.
 *
 * <p>RN04 / [NOVO] - protecao de integridade referencial ({@code ON DELETE RESTRICT}).
 */
public class EventCategoryInUseException extends RuntimeException {

    public EventCategoryInUseException() {
        super("Não é possível excluir a categoria pois existem eventos associados a ela.");
    }
}
