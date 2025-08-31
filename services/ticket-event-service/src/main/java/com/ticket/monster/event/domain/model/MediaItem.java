package com.ticket.monster.event.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "media_item")
public class MediaItem {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "media_item_seq")
    @SequenceGenerator(name = "media_item_seq", sequenceName = "media_item_seq", allocationSize = 1, initialValue = 1)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID uuid;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private String mediaType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id")
    private Venue venue;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public MediaItem() {
    }

    public MediaItem(String url, String mediaType, Event event) {
        this.uuid = UUID.randomUUID();
        this.url = url;
        this.mediaType = mediaType;
        this.event = event;
    }

    public MediaItem(String url, String mediaType, Venue venue) {
        this.uuid = UUID.randomUUID();
        this.url = url;
        this.mediaType = mediaType;
        this.venue = venue;
    }

    public Long getId() {
        return id;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getUrl() {
        return url;
    }

    public String getMediaType() {
        return mediaType;
    }

    public Event getEvent() {
        return event;
    }

    public Venue getVenue() {
        return venue;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

}