package com.example.venueservice.controller;

import com.example.venueservice.entity.VenueReservation;
import com.example.venueservice.service.VenueReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/venues/{venueId}/reservations")
public class VenueReservationController {

    @Autowired
    private VenueReservationService venueReservationService;

    @PostMapping
    public ResponseEntity<VenueReservation> createReservation(@PathVariable UUID venueId, @RequestBody VenueReservation reservation) {
        reservation.setVenueId(venueId);
        VenueReservation createdReservation = venueReservationService.createReservation(reservation);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdReservation);
    }

    @GetMapping
    public ResponseEntity<List<VenueReservation>> getAllReservations(@PathVariable UUID venueId) {
        List<VenueReservation> reservations = venueReservationService.getAllReservationsByVenueId(venueId);
        return ResponseEntity.ok(reservations);
    }

    @GetMapping("/{reservationId}")
    public ResponseEntity<VenueReservation> getReservationById(@PathVariable UUID venueId, @PathVariable UUID reservationId) {
        VenueReservation reservation = venueReservationService.getReservationById(reservationId);
        return ResponseEntity.ok(reservation);
    }

    @PatchMapping("/{reservationId}")
    public ResponseEntity<VenueReservation> updateReservation(@PathVariable UUID venueId, @PathVariable UUID reservationId, @RequestBody VenueReservation reservation) {
        reservation.setId(reservationId);
        reservation.setVenueId(venueId);
        VenueReservation updatedReservation = venueReservationService.updateReservation(reservation);
        return ResponseEntity.ok(updatedReservation);
    }

    @DeleteMapping("/{reservationId}")
    public ResponseEntity<Void> deleteReservation(@PathVariable UUID venueId, @PathVariable UUID reservationId) {
        venueReservationService.deleteReservation(reservationId);
        return ResponseEntity.noContent().build();
    }
}