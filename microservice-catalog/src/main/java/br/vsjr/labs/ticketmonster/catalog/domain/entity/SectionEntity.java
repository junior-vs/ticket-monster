package br.vsjr.labs.ticketmonster.catalog.domain.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "section", schema = "catalog")
public class SectionEntity extends PanacheEntityBase {

    @Id
    @Column(nullable = false, updatable = false)
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false)
    public VenueEntity venue;

    @Column(nullable = false, length = 120)
    public String name;

    @Column(name = "number_of_rows", nullable = false)
    public int numberOfRows;

    @Column(name = "row_capacity", nullable = false)
    public int rowCapacity;

    @Column(insertable = false, updatable = false)
    public Integer capacity;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
