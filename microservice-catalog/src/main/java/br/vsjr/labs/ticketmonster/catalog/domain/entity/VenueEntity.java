package br.vsjr.labs.ticketmonster.catalog.domain.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "venue", schema = "catalog")
public class VenueEntity extends PanacheEntityBase {

    @Id
    @Column(nullable = false, updatable = false)
    public UUID id;

    @Column(nullable = false, length = 255, unique = true)
    public String name;

    @Column(columnDefinition = "TEXT")
    public String description;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "addressLine", column = @Column(name = "address_line", length = 255)),
        @AttributeOverride(name = "city", column = @Column(name = "city", length = 120)),
        @AttributeOverride(name = "state", column = @Column(name = "state", length = 120)),
        @AttributeOverride(name = "postalCode", column = @Column(name = "postal_code", length = 20)),
        @AttributeOverride(name = "country", column = @Column(name = "country", length = 120))
    })
    public VenueAddressEmbeddable address = new VenueAddressEmbeddable();

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @OneToMany(mappedBy = "venue", cascade = CascadeType.ALL, orphanRemoval = true)
    public Set<SectionEntity> sections = new LinkedHashSet<>();

    @OneToMany(mappedBy = "venue")
    public Set<ShowEntity> shows = new LinkedHashSet<>();

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
