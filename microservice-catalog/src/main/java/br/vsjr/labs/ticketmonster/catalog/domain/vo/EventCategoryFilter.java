package br.vsjr.labs.ticketmonster.catalog.domain.vo;

import br.vsjr.labs.ticketmonster.catalog.domain.exception.InvalidEventCategoryException;

import java.util.Optional;
import java.util.UUID;

public record EventCategoryFilter(
        Optional<UUID> eventCategoryID
) {


    public static EventCategoryFilter of(String categoryIdRaw) throws InvalidEventCategoryException {
        if (categoryIdRaw == null || categoryIdRaw.isBlank()) {
            return new EventCategoryFilter(Optional.empty());
        }
        try {
            return new EventCategoryFilter(Optional.of(UUID.fromString(categoryIdRaw.trim())));
        } catch (IllegalArgumentException e) {
            throw new InvalidEventCategoryException("O identificador de categoria informado é sintaticamente inválido: " + categoryIdRaw);
        }
    }
}
