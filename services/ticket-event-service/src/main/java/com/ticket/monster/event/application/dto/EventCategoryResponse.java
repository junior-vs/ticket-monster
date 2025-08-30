package com.ticket.monster.event.application.dto;

// src/main/java/com/ticket/monster/event/application/dto/EventCategoryResponse.java


import java.time.LocalDateTime;

public record EventCategoryResponse(
    Long id,
    String description,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}