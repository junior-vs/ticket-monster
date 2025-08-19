package com.ticket.monster.event.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

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

    // Getters e Setters
    // ...
}

