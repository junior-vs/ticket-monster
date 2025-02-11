package com.example.venueservice.service;

import com.example.venueservice.entity.VenueReservation;
import com.example.venueservice.repository.VenueReservationRepository;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class VenueReservationService {

    @Inject
    VenueReservationRepository venueReservationRepository;

    public List<VenueReservation> findAll() {
        return venueReservationRepository.findAll().list();
    }

    public Optional<VenueReservation> findById(String id) {
        return venueReservationRepository.findByIdOptional(id);
    }

    @Transactional
    public VenueReservation create(VenueReservation venueReservation) {
        venueReservationRepository.persist(venueReservation);
        return venueReservation;
    }

    @Transactional
    public VenueReservation update(String id, VenueReservation venueReservation) {
        VenueReservation existingReservation = venueReservationRepository.findById(id);
        if (existingReservation != null) {
            existingReservation.setVenueId(venueReservation.getVenueId());
            existingReservation.setEventId(venueReservation.getEventId());
            existingReservation.setReservedBy(venueReservation.getReservedBy());
            existingReservation.setStartDatetime(venueReservation.getStartDatetime());
            existingReservation.setEndDatetime(venueReservation.getEndDatetime());
            existingReservation.setStatus(venueReservation.getStatus());
            return existingReservation;
        }
        return null;
    }

    @Transactional
    public void delete(String id) {
        venueReservationRepository.deleteById(id);
    }
}