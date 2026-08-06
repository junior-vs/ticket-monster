package br.vsjr.labs.ticketmoster.catalog.domain.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "media_type_catalog", schema = "catalog")
public class MediaTypeCatalogEntity extends PanacheEntityBase {

    @Id
    @Column(nullable = false, length = 30)
    public String code;

    @Column(nullable = false, length = 120)
    public String description;

    @Column(nullable = false)
    public boolean enabled = true;

    @OneToMany(mappedBy = "mediaType")
    public Set<MediaItemEntity> mediaItems = new LinkedHashSet<>();
}
