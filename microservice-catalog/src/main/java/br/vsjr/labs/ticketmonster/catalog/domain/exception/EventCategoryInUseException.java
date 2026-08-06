package br.vsjr.labs.ticketmonster.catalog.domain.exception;

public class EventCategoryInUseException extends RuntimeException {

    public EventCategoryInUseException(String id) {
        super("Categoria com id '" + id + "' possui um ou mais eventos vinculados.");
    }
}
