package br.studio.ticketmonster.midiaitem;

import static jakarta.persistence.EnumType.STRING;

import java.time.LocalDateTime;

import org.hibernate.validator.constraints.URL;

import br.studio.ticketmonster.infra.Default;
import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;

@Entity
public class MidiaItem extends PanacheEntity {

    @Enumerated(STRING)
    private MediaType mediaType;

    @Column(unique = true, nullable = false)
    @URL
    private String url;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public MidiaItem() {
    }

    @Default
    public MidiaItem(MediaType mediaType, String url, String description){
        this.mediaType = mediaType;
        this.url = url;
        this.description = description;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

    }

    public void update(MidiaItem input) {
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
