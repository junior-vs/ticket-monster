package com.ticket.monster.event.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.ticket.monster.event.application.dto.EventCategoryRequest;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "event_category")
public class EventCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "event_category_seq")
    @SequenceGenerator(name = "event_category_seq", sequenceName = "event_category_seq", allocationSize = 1, initialValue = 1)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String description;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = false, fetch = FetchType.LAZY)
    private List<Event> events = new ArrayList<>();

    public EventCategory(String description) {
        this.description = description;
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
        return this;
    }
}