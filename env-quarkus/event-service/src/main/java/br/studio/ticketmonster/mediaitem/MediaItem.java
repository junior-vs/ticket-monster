package br.studio.ticketmonster.mediaitem;

import static jakarta.persistence.EnumType.STRING;

import java.time.LocalDateTime;

import org.hibernate.validator.constraints.URL;

import br.studio.ticketmonster.event.Event;
import br.studio.ticketmonster.infra.Default;
import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class MediaItem extends PanacheEntity {

    @Enumerated(STRING)
    private MediaType mediaType;

    @Column(unique = true, nullable = false)
    @URL
    private String url;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    public MediaItem() {
    }

    @Default
    public MediaItem(MediaType mediaType, String url, String description, Event event) {
        this.mediaType = mediaType;
        this.url = url;
        this.description = description;
        this.event = event;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public void update(MediaItem input) {
        this.mediaType = input.getMediaType();
        this.url = input.getUrl();
        this.description = input.getDescription();
        this.updatedAt = LocalDateTime.now();
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public String getUrl() {
        return url;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

}
