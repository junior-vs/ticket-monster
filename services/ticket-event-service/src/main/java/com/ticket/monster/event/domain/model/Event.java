package com.ticket.monster.event.domain.model;

import com.ticket.monster.event.infrastructure.mapping.Default;
import jakarta.persistence.*;
import org.mapstruct.Builder;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import java.util.Objects;


@Entity
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    @Column(nullable = false)
    private String location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private EventCategory category;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MediaItem> mediaItems;


    public Event() {
    }


    /**
     * Construtor privado para garantir imutabilidade e uso do Builder.
     */

    private Event(Builder builder) {

        this.uuid = builder.uuid;
        this.name = builder.name;
        this.description = builder.description;
        this.startDate = builder.startDate;
        this.endDate = builder.endDate;
        this.location = builder.location;
        this.category = builder.category;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
        this.mediaItems = builder.mediaItems;
    }

    /**
     * Builder para Event, garantindo validação dos campos obrigatórios.
     */

    public static class Builder {

        private UUID uuid;
        private String name;
        private String description;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private String location;
        private EventCategory category;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private List<MediaItem> mediaItems;



        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder startDate(LocalDateTime startDate) { this.startDate = startDate; return this; }
        public Builder endDate(LocalDateTime endDate) { this.endDate = endDate; return this; }
        public Builder location(String location) { this.location = location; return this; }
        public Builder category(EventCategory category) { this.category = category; return this; }

        public Builder mediaItems(List<MediaItem> mediaItems) { this.mediaItems = mediaItems; return this; }

        /**
         * Cria uma instância de Event validando os campos obrigatórios.
         * @throws NullPointerException se algum campo obrigatório estiver ausente.
         */
        public Event build() {
            Objects.requireNonNull(UUID.randomUUID(), "uuid não pode ser nulo");
            Objects.requireNonNull(name, "name não pode ser nulo");
            Objects.requireNonNull(startDate, "startDate não pode ser nulo");
            Objects.requireNonNull(endDate, "endDate não pode ser nulo");
            Objects.requireNonNull(location, "location não pode ser nulo");
            Objects.requireNonNull(category, "category não pode ser nulo");
            Objects.requireNonNull(LocalDateTime.now());
            Objects.requireNonNull(LocalDateTime.now());
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

    public String getLocation() {
        return location;
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
