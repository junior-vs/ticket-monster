package br.vsjr.labs.ticketmonster.catalog.domain.exception;

public class EventCategoryNotFoundException extends RuntimeException {
    public EventCategoryNotFoundException(String message) {
        super(message);
    }
}
