package com.ticket.monster.event.infrastructure.repository;

import com.ticket.monster.event.domain.model.Event;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class EventRepository implements PanacheRepositoryBase<Event, Long> {



}
