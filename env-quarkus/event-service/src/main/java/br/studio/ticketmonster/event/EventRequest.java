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

    /*
     * {
     * "name": "Rock in Rio",
     * "description": "Maior festival de música do Brasil",
     * "category": "Música",
     * "startDate": "2025-09-27T20:00:00",
     * "endDate": "2025-09-30T23:59:59",
     * "location": "Rio de Janeiro",
     * "imageUrl": "https://example.com/rockinrio.jpg"
     * }
     */

}
