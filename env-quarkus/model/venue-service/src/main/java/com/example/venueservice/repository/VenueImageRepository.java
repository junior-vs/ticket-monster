package com.example.venueservice.repository;

import com.example.venueservice.entity.VenueImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VenueImageRepository extends JpaRepository<VenueImage, UUID> {
}