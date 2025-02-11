package com.example.venueservice.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "venues")
public class Venue {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private String country = "Brasil";

    @Column(name = "postal_code", nullable = false)
    private String postalCode;

    private BigDecimal latitude;

    private BigDecimal longitude;

    @Column(nullable = false)
    private int capacity;

    @Column(name = "venue_type", nullable = false)
    private String venueType;

    @Column(columnDefinition = "jsonb")
    private Map<String, Object> amenities;

    @Column(nullable = false)
    private String status = "ativo";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Getters and Setters
}