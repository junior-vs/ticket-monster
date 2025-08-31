package com.ticket.monster.event.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.ticket.monster.event.application.dto.EventRequest;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "event_seq")
    @SequenceGenerator(name = "event_seq", sequenceName = "event_seq", allocationSize = 1, initialValue = 1)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID uuid;

    @Column(nullable = false)
    private String name;

    @Column(length = 2048)
    private String description;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private EventCategory category;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MediaItem> mediaItems = new ArrayList<>();

    public Event() {
    }

    public Event update(EventRequest dto, EventCategory category) {
        this.name = dto.name();
        this.description = dto.description();
        this.startDate = dto.startDate();
        this.endDate = dto.endDate();
        this.category = category;
        return this;
    }

    public static Builder builder() {
        return new Builder();
    }

    private Event(Builder builder) {
        this.uuid = UUID.randomUUID();
        this.name = Objects.requireNonNull(builder.name, "name não pode ser nulo");
        this.description = builder.description;
        this.startDate = Objects.requireNonNull(builder.startDate, "startDate não pode ser nulo");
        this.endDate = Objects.requireNonNull(builder.endDate, "endDate não pode ser nulo");
        this.category = Objects.requireNonNull(builder.category, "category não pode ser nulo");
        this.mediaItems = builder.mediaItems;
    }

    public static class Builder {
        private String name;
        private String description;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private EventCategory category;
        private List<MediaItem> mediaItems = new ArrayList<>();

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder startDate(LocalDateTime startDate) {
            this.startDate = startDate;
            return this;
        }

        public Builder endDate(LocalDateTime endDate) {
            this.endDate = endDate;
            return this;
        }

        public Builder category(EventCategory category) {
            this.category = category;
            return this;
        }

        public Builder addMediaItem(MediaItem mediaItem) {
            this.mediaItems.add(mediaItem);
            return this;
        }

        public Event build() {
            return new Event(this);
        }
    }

    public Long getId() {
        return id;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public EventCategory getCategory() {
        return category;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<MediaItem> getMediaItems() {
        return mediaItems;
    }
}