package br.vsjr.labs.ticketmoster.catalog.adapter.in.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(
    @NotBlank
    @Size(max = 120)
    String description
) {
}
