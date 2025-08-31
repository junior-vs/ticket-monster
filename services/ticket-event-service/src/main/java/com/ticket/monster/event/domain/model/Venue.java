package com.ticket.monster.event.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;

@Entity
public class Venue {

    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "venue_seq")
    @SequenceGenerator(name = "venue_seq", sequenceName = "venue_seq", allocationSize = 1, initialValue = 1)
    private Long id;

    @Column(nullable = false, unique = true)
    private  String name;

    @Column
    private  String description;

    @Column
    @Embedded
    private  Address address;

    @Column
    private  Integer capacity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "picture_id")
    private  MediaItem pictureCover;

    @Column(nullable = false)
    private  Long version;

    public Venue() {
    }

    private Venue(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.description = builder.description;
        this.address = builder.address;
        this.capacity = builder.capacity;
        this.pictureCover = builder.pictureCover;
        this.version = builder.version;
    }

    public Long getId() {
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

    public Integer getCapacity() {
        return capacity;
    }

    public MediaItem getPictureCover() {
        return pictureCover;
    }

    public Long getVersion() {
        return version;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long id;
        private String name;
        private String description;
        private Address address;
        private Integer capacity;
        private MediaItem pictureCover;
        private Long version;

        private Builder() {}

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder address(Address address) {
            this.address = address;
            return this;
        }

        public Builder capacity(Integer capacity) {
            this.capacity = capacity;
            return this;
        }

        public Builder pictureCover(MediaItem pictureCover) {
            this.pictureCover = pictureCover;
            return this;
        }

        public Builder version(Long version) {
            this.version = version;
            return this;
        }

        public Venue build() {
            return new Venue(this);
        }
    }

}
