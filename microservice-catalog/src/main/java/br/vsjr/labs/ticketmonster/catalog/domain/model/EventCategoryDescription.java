package br.vsjr.labs.ticketmonster.catalog.domain.model;


import jakarta.validation.constraints.NotBlank;

import java.util.Objects;
// RN06: Encapsula a regra de normalização da descrição da categoria (trim + imutabilidade)
public record EventCategoryDescription(@NotBlank(message = "A descrição da categoria é obrigatória.") String value) {

    public EventCategoryDescription {
        Objects.requireNonNull(value, "A descrição da categoria é obrigatória.");
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("A descrição da categoria não pode ser vazia ou contida apenas por espaços.");
        }
        if (value.length() < 2 || value.length() > 50) {
            throw new IllegalArgumentException("A descrição da categoria deve ter entre 2 e 50 caracteres.");
        }
    }
    public String normalizedForComparison() {
        return value.toLowerCase();
    }
}