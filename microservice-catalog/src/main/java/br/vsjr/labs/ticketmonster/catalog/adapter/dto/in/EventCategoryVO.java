package br.vsjr.labs.ticketmonster.catalog.adapter.dto.in;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;

public record EventCategoryVO(UUID id, String description, Instant createdAt) {

    public EventCategoryVO(@NotBlank @Max(value=200) String description) {
        this(UUID.randomUUID(), description, Instant.now());
    }
}
