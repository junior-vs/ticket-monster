package com.ticket.monster.event.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO imutável para transferência de dados de eventos.
 * Utiliza Java record para simplificar e garantir imutabilidade.
 */
public record EventResponse(
    Long id,
    UUID uuid,
    String name,
    String description,
    LocalDateTime startDate,
    LocalDateTime endDate,
    String location,
    Long categoryId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
