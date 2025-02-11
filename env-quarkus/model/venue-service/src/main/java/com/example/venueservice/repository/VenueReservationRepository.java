package com.example.venueservice.repository;

import com.example.venueservice.entity.VenueReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VenueReservationRepository extends JpaRepository<VenueReservation, UUID> {
    // Additional query methods can be defined here if needed
}