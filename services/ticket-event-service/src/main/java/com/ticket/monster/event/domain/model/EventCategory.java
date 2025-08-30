package com.ticket.monster.event.domain.model;

import com.ticket.monster.event.application.dto.EventCategoryRequest;
import com.ticket.monster.event.infrastructure.mapping.Default;
import io.quarkus.hibernate.orm.runtime.Hibernate;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class EventCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = false, fetch = FetchType.LAZY)
    private List<Event> events;

    @Default
    public EventCategory(String description) {
        this.description = description;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.events = new ArrayList<>();
    }

    public EventCategory() {
    }

    public Long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<Event> getEvents() {
        return events;
    }

    @Override
    public String toString() {
        return "EventCategory{" +
                "updatedAt=" + updatedAt +
                ", createdAt=" + createdAt +
                ", description='" + description + '\'' +
                ", id=" + id +
                '}';
    }

    public EventCategory update(@Valid EventCategoryRequest request) {
        this.description = request.description();
        this.updatedAt = LocalDateTime.now();
        return  this;

    }
}

