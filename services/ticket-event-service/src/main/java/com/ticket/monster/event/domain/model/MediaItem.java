package com.ticket.monster.event.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
public class MediaItem {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

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

    // Getters e Setters
    // ...
}

