package com.ticket.monster.event.application.dto;
// src/main/java/com/ticket/monster/event/application/dto/EventCategoryRequest.java

import jakarta.validation.constraints.NotBlank;

public record EventCategoryRequest(@NotBlank(message = "Descrição é obrigatória") String description) {
}