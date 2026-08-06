package br.vsjr.labs.ticketmonster.catalog.domain.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "media_item", schema = "catalog")
public class MediaItemEntity extends PanacheEntityBase {

    @Id
    @Column(nullable = false, updatable = false)
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "media_type_code", nullable = false)
    public MediaTypeCatalogEntity mediaType;

    @Column(nullable = false, length = 2048, unique = true)
    public String url;

    @Column(name = "cached_file_name", length = 255)
    public String cachedFileName;

    @Column(name = "fallback_applied", nullable = false)
    public boolean fallbackApplied;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @OneToMany(mappedBy = "mediaItem")
    public Set<EventEntity> events = new LinkedHashSet<>();

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
