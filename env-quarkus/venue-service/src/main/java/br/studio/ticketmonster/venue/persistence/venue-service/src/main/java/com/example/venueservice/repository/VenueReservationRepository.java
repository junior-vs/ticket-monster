package com.example.venueservice.repository;

import com.example.venueservice.entity.VenueReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VenueReservationRepository extends JpaRepository<VenueReservation, UUID> {
}