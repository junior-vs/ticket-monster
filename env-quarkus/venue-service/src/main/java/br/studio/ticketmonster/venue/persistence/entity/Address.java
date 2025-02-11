package br.studio.ticketmonster.venue.persistence.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Address {

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private String country = "Brasil";

    @Column(name = "postal_code", nullable = false)
    private String postalCode;

    private BigDecimal latitude;

    private BigDecimal longitude;


    // Constructors, getters, and setters

    public Address() {
    }

    public Address(String address, String city, String state, String country, String postalCode, BigDecimal latitude,
            BigDecimal longitude) {
        this.address = address;
        this.city = city;
        this.state = state;
        this.country = country;
        this.postalCode = postalCode;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getAddress() {
        return address;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getCountry() {
        return country;
    }

    public String getPostalCode() {
        return postalCode;
    }
    
    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public static class Builder {
        private String address;
        private String city;
        private String state;
        private String country = "Brasil";
        private String postalCode;
        private BigDecimal latitude;
        private BigDecimal longitude;

        public Builder withAddress(String address) {
            this.address = address;
            return this;
        }

        public Builder withCity(String city) {
            this.city = city;
            return this;
        }

        public Builder withState(String state) {
            this.state = state;
            return this;
        }

        public Builder withCountry(String country) {
            this.country = country;
            return this;
        }

        public Builder withPostalCode(String postalCode) {
            this.postalCode = postalCode;
            return this;
        }

        public Builder withLatitude(BigDecimal latitude) {
            this.latitude = latitude;
            return this;
        }

        public Builder withLongitude(BigDecimal longitude) {
            this.longitude = longitude;
            return this;
        }

        public Address build() {
            return new Address(address, city, state, country, postalCode, latitude, longitude);
        }
    }
}
