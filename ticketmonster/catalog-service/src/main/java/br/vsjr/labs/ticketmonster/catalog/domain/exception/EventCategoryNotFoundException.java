package br.vsjr.labs.ticketmonster.catalog.domain.exception;

import java.util.UUID;

/**
 * Lancada quando uma categoria referenciada por id nao existe.
 */
public class EventCategoryNotFoundException extends RuntimeException {

    public EventCategoryNotFoundException(UUID id) {
        super("Categoria de evento não encontrada para o id " + id + ".");
    }
}
