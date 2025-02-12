package br.studio.ticketmonster.venue.persistence.entity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "venues")
public class Venues {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Embedded
    private Address address;

    @Column(nullable = false)
    private int capacity;

    @Column(name = "venue_type", nullable = false)
    private VenueTypeEnum venueType;

    @Column(columnDefinition = "jsonb")
    private String amenities;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private StatusVenueEnum status;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "venues", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Images> images;

    @OneToMany(mappedBy = "venues", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Sectors> sectors;

    @OneToMany(mappedBy = "venues", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Reservations> reservations;

    @OneToMany(mappedBy = "venues", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Reviews> reviews;

    /**
     * for JPA
     * 
     */
    @Deprecated(since = "1.0.0")
    public Venues() { // for JPA
    }

    public Venues(String name, String description, Address address, int capacity, VenueTypeEnum venueType,
            String amenities, StatusVenueEnum status) {
        this.name = name;
        this.description = description;
        this.address = address;
        this.capacity = capacity;
        this.venueType = venueType;
        this.amenities = amenities;
        this.status = status;
    }
    

    public static class Builder {
        private String name;
        private String description;
        private Address address;
        private int capacity;
        private VenueTypeEnum venueType;
        private String amenities;
        private StatusVenueEnum status;

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder withAddress(Address address) {
            this.address = address;
            return this;
        }

        public Builder withCapacity(int capacity) {
            this.capacity = capacity;
            return this;
        }

        public Builder withVenueType(VenueTypeEnum venueType) {
            this.venueType = venueType;
            return this;
        }

        public Builder withAmenities(String amenities) {
            this.amenities = amenities;
            return this;
        }

        public Builder withStatus(StatusVenueEnum status) {
            this.status = status;
            return this;
        }

        public Venues build() {
            return new Venues(name, description, address, capacity, venueType, amenities, status);
        }
    }

    public List<Reservations> getReservations() {
        return reservations;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Address getAddress() {
        return address;
    }

    public int getCapacity() {
        return capacity;
    }

    public VenueTypeEnum getVenueType() {
        return venueType;
    }

    public String getAmenities() {
        return amenities;
    }

    public StatusVenueEnum getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<Images> getImages() {
        return images;
    }

    public List<Sectors> getSectors() {
        return sectors;
    }

    public List<Reviews> getReviews() {
        return reviews;
    }

    

}