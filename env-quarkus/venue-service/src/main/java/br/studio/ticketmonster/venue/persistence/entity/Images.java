package br.studio.ticketmonster.venue.persistence.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "images")
public class Images {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "venue_id", nullable = false)
    private Venues venues;

    @Column(name = "url", nullable = false)
    private String url;

    @Column(name = "description")
    private String description;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    private Images(Venues venues, String url, String description) {
        this.venues = venues;
        this.url = url;
        this.description = description;
        this.uploadedAt = LocalDateTime.now();
    }

    public static Images create(Venues venues, String url, String description) {
        return new Images(venues, url, description);
    }

    /**
     * @deprecated hibernate eyes only
     */
    @Deprecated(since = "1.0")
    public Images() { // hibernate eyes only
    }

    public UUID getId() {
        return id;
    }

    public UUID getVenuesId() {
        return venues.getId();
    }

    public String getUrl() {
        return url;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

}