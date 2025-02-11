package br.studio.ticketmonster.venue.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reservations")
public class Reservations {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venues venue;

    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "reserved_by", nullable = false)
    private UUID reservedBy;

    @Column(name = "start_datetime", nullable = false)
    private LocalDateTime startDatetime;

    @Column(name = "end_datetime", nullable = false)
    private LocalDateTime endDatetime;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Reservations() {
    }

    public Reservations(UUID id, Venues venue, UUID eventId, UUID reservedBy, LocalDateTime startDatetime,
            LocalDateTime endDatetime, String status) {
        this.id = id;
        this.venue = venue;
        this.eventId = eventId;
        this.reservedBy = reservedBy;
        this.startDatetime = startDatetime;
        this.endDatetime = endDatetime;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public Venues getVenue() {
        return venue;
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getReservedBy() {
        return reservedBy;
    }

    public LocalDateTime getStartDatetime() {
        return startDatetime;
    }

    public LocalDateTime getEndDatetime() {
        return endDatetime;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}