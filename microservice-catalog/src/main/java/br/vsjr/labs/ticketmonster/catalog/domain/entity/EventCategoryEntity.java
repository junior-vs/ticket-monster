package br.vsjr.labs.ticketmonster.catalog.domain.entity;

import br.vsjr.labs.ticketmonster.catalog.domain.model.EventCategory;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "event_category", schema = "catalog")
public class EventCategoryEntity extends PanacheEntityBase {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 120, unique = true)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "eventCategory")
    private Set<EventEntity> events = new LinkedHashSet<>();

    // Default constructor required by JPA
    public EventCategoryEntity() {
    }

    public EventCategoryEntity(EventCategory category) {
        this.id = category.id().value();
        this.description = category.description().value();
        this.createdAt = category.createdAt();
        //this.events = category.events();
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Set<EventEntity> getEvents() {
        return events;
    }
}
