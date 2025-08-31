package com.ticket.monster.event.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

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
    private String mediaType; // Enum pode ser criado posteriormente

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;


    public MediaItem() {
    }

    public MediaItem(UUID uuid, String url, String mediaType, Event event) {
        this.uuid = uuid;
        this.url = url;
        this.mediaType = mediaType;
        this.event = event;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }


    

}

