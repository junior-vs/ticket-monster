package br.studio.ticketmonster.category;

import jakarta.validation.constraints.NotBlank;

public record EventCategoryRequest(@NotBlank String description) {
}
