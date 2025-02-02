package br.studio.ticketmonster.event;

import java.time.LocalDate;

import io.smallrye.common.constraint.NotNull;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;

public record EventRequest(
        @NotBlank String name,
        @NotBlank String description,
        @NotBlank String category,
        @NotNull  @FutureOrPresent LocalDate startDate,
        @NotNull  @FutureOrPresent LocalDate endDate,
        String location,
        String imageUrl) {

}
