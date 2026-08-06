package br.vsjr.labs.ticketmoster.catalog.domain.exception;

public class InvalidEventCategoryInputException extends RuntimeException {

    public InvalidEventCategoryInputException(String message) {
        super(message);
    }
}
