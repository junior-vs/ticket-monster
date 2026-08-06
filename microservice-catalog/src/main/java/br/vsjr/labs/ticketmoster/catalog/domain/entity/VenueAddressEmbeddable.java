package br.vsjr.labs.ticketmoster.catalog.domain.entity;

import jakarta.persistence.Embeddable;

@Embeddable
public class VenueAddressEmbeddable {

    public String addressLine;
    public String city;
    public String state;
    public String postalCode;
    public String country;
}
