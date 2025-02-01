package br.studio.ticketmonster.event;


import java.time.LocalDate;
import io.smallrye.common.constraint.NotNull;
import jakarta.validation.constraints.NotBlank;

public record EventResponse(


        @NotNull Long id,
        @NotBlank String name,
        @NotBlank String description,
        @NotBlank String category,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        String location,
        String imageUrl) {


            
}
