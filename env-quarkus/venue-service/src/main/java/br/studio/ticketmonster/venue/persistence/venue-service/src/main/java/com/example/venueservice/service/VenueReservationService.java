package com.example.venueservice.service;

import com.example.venueservice.entity.VenueReservation;
import com.example.venueservice.repository.VenueReservationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class VenueReservationService {

    @Inject
    VenueReservationRepository venueReservationRepository;

    public VenueReservation createReservation(VenueReservation reservation) {
        return venueReservationRepository.save(reservation);
    }

    public Optional<VenueReservation> getReservationById(String id) {
        return venueReservationRepository.findById(id);
    }

    public List<VenueReservation> getAllReservations() {
        return venueReservationRepository.findAll();
    }

    public VenueReservation updateReservation(String id, VenueReservation updatedReservation) {
        if (venueReservationRepository.existsById(id)) {
            updatedReservation.setId(id);
            return venueReservationRepository.save(updatedReservation);
        }
        return null;
    }

    public boolean deleteReservation(String id) {
        if (venueReservationRepository.existsById(id)) {
            venueReservationRepository.deleteById(id);
            return true;
        }
        return false;
    }
}