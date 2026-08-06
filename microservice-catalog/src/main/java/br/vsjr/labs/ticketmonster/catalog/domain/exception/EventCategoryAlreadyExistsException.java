package br.vsjr.labs.ticketmonster.catalog.domain.exception;

public class EventCategoryAlreadyExistsException extends RuntimeException {

    public EventCategoryAlreadyExistsException(String description) {
        super("Ja existe uma categoria cadastrada com a descricao '" + description + "'.");
    }
}
