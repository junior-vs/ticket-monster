package com.ticket.monster.event.infrastructure.repository;

import com.ticket.monster.event.domain.model.EventCategory;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class EventCategoryRepository implements PanacheRepositoryBase<EventCategory, Long> {
}
