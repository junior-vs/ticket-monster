package com.ticket.monster.event.domain.model;

import jakarta.persistence.Embeddable;

@Embeddable
public class Address {

    private String street;
    private String city;
    private String country;
    private String zipCode;
    private String state;

    public Address() {
    }

    private Address(Builder builder) {
        this.street = builder.street;
        this.city = builder.city;
        this.country = builder.country;
        this.zipCode = builder.zipCode;
        this.state = builder.state;
    }

    public String getStreet() {
        return street;
    }
    public String getCity() {
        return city;
    }
    public String getCountry() {
        return country;
    }
    public String getZipCode() {
        return zipCode;
    }
    public String getState() {
        return state;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String street;
        private String city;
        private String country;
        private String zipCode;
        private String state;

        private Builder() {}

        public Builder street(String street) {
            this.street = street;
            return this;
        }

        public Builder city(String city) {
            this.city = city;
            return this;
        }

        public Builder country(String country) {
            this.country = country;
            return this;
        }

        public Builder zipCode(String zipCode) {
            this.zipCode = zipCode;
            return this;
        }

        public Builder state(String state) {
            this.state = state;
            return this;
        }

        public Address build() {
            return new Address(this);
        }
    }
}
