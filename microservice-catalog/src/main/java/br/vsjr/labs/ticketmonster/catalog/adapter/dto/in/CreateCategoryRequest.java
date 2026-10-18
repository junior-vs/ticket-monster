package br.vsjr.labs.ticketmonster.catalog.adapter.dto.in;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
        @NotBlank
        @Size(max = 120)
        String description
) {
}
