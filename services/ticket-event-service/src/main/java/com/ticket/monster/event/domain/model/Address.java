package com.ticket.monster.event.domain.model;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Embeddable
public class Address {

    @NotNull
    @Size(min = 1)
    private String street;
    @NotNull
    @Size(min = 1)
    private String city;
    @NotNull
    @Size(min = 1)
    private String country;
    private String zipCode;
    private String state;

    public Address() {
    }

    public Address(String street, String city, String country, String zipCode, String state) {
        this.street = street;
        this.city = city;
        this.country = country;
        this.zipCode = zipCode;
        this.state = state;
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
}