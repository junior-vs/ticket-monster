package com.example.venueservice.service;

import com.example.venueservice.entity.Venue;
import com.example.venueservice.repository.VenueRepository;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class VenueService {

    @Inject
    VenueRepository venueRepository;

    public List<Venue> findAll() {
        return venueRepository.findAll().list();
    }

    public Optional<Venue> findById(String id) {
        return venueRepository.findByIdOptional(id);
    }

    @Transactional
    public Venue create(Venue venue) {
        venueRepository.persist(venue);
        return venue;
    }

    @Transactional
    public Venue update(String id, Venue venue) {
        Venue existingVenue = venueRepository.findById(id);
        if (existingVenue != null) {
            existingVenue.setName(venue.getName());
            existingVenue.setDescription(venue.getDescription());
            existingVenue.setAddress(venue.getAddress());
            existingVenue.setCity(venue.getCity());
            existingVenue.setState(venue.getState());
            existingVenue.setCountry(venue.getCountry());
            existingVenue.setPostalCode(venue.getPostalCode());
            existingVenue.setLatitude(venue.getLatitude());
            existingVenue.setLongitude(venue.getLongitude());
            existingVenue.setCapacity(venue.getCapacity());
            existingVenue.setVenueType(venue.getVenueType());
            existingVenue.setAmenities(venue.getAmenities());
            existingVenue.setStatus(venue.getStatus());
            existingVenue.setUpdatedAt(venue.getUpdatedAt());
            return existingVenue;
        }
        return null;
    }

    @Transactional
    public void delete(String id) {
        venueRepository.deleteById(id);
    }
}