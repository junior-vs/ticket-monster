package br.vsjr.labs.ticketmoster.catalog.domain.model;


import jakarta.validation.constraints.NotNull;

import java.util.Objects;
import java.util.UUID;

public record EventCategoryId(@NotNull(message = "O UUID da categoria não pode ser nulo.") UUID value) {
    public EventCategoryId {
        Objects.requireNonNull(value, "O UUID da categoria não pode ser nulo.");
    }
    public static EventCategoryId generate() {
        return new EventCategoryId(UUID.randomUUID());
    }
    public static EventCategoryId from(String rawUuid) {
        return new EventCategoryId(UUID.fromString(rawUuid));
    }
}