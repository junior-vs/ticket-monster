package br.vsjr.labs.ticketmoster.catalog.domain.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "event", schema = "catalog")
public class EventEntity extends PanacheEntityBase {

    @Id
    @Column(nullable = false, updatable = false)
    public UUID id;

    @Column(nullable = false, length = 50, unique = true)
    public String name;

    @Column(nullable = false, length = 1000)
    public String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_category_id", nullable = false)
    public EventCategoryEntity eventCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_item_id")
    public MediaItemEntity mediaItem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public EventStatus status = EventStatus.DRAFT;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    @Column(name = "published_at")
    public Instant publishedAt;

    @OneToMany(mappedBy = "event")
    public Set<ShowEntity> shows = new LinkedHashSet<>();

    @PrePersist
    void prePersist() {
        var now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (status == null) {
            status = EventStatus.DRAFT;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
