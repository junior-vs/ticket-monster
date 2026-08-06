package br.vsjr.labs.ticketmonster.catalog.domain.exception;

public class InvalidEventCategoryInputException extends RuntimeException {

    public InvalidEventCategoryInputException(String message) {
        super(message);
    }
}
