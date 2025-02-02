package br.studio.ticketmonster.event;

import java.time.LocalDate;
import java.time.LocalDateTime;

import io.smallrye.common.constraint.NotNull;
import jakarta.validation.constraints.NotBlank;

public record EventResponse(
                Long id,
                String name,
                String description,
                String category,
                LocalDate startDate,
                LocalDate endDate,
                String location,
                String imageUrl,
                String uuid,
                LocalDateTime createdAt,
                LocalDateTime updatedAt) {

}
