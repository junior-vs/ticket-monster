package com.example.venueservice.repository;

import com.example.venueservice.entity.VenueReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VenueReviewRepository extends JpaRepository<VenueReview, UUID> {
}