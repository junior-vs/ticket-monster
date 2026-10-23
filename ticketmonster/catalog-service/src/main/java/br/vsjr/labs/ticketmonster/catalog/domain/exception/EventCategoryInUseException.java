package br.vsjr.labs.ticketmonster.catalog.domain.exception;

import java.util.UUID;

/**
 * Lancada ao tentar excluir uma categoria que possui eventos associados.
 *
 * <p>RN04 / [NOVO] - protecao de integridade referencial ({@code ON DELETE RESTRICT}).
 */
public class EventCategoryInUseException extends RuntimeException {

    public EventCategoryInUseException(UUID id) {
        super("Não é possível excluir a categoria " + id + " pois existem eventos associados a ela.");
    }
}
