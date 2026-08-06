package br.vsjr.labs.ticketmoster.catalog.domain.exception;

public class EventCategoryNotFoundException extends RuntimeException {
    public EventCategoryNotFoundException(String message) {
        super(message);
    }
}
