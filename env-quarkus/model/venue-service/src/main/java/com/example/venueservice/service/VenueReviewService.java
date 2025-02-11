package com.example.venueservice.service;

import com.example.venueservice.entity.VenueReview;
import com.example.venueservice.repository.VenueReviewRepository;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class VenueReviewService {

    @Inject
    VenueReviewRepository venueReviewRepository;

    public List<VenueReview> findAll() {
        return venueReviewRepository.findAll().list();
    }

    public Optional<VenueReview> findById(String id) {
        return venueReviewRepository.findByIdOptional(id);
    }

    @Transactional
    public VenueReview create(VenueReview venueReview) {
        venueReviewRepository.persist(venueReview);
        return venueReview;
    }

    @Transactional
    public VenueReview update(String id, VenueReview venueReview) {
        VenueReview existingReview = venueReviewRepository.findById(id);
        if (existingReview != null) {
            existingReview.setRating(venueReview.getRating());
            existingReview.setComment(venueReview.getComment());
            existingReview.setCreatedAt(venueReview.getCreatedAt());
            return existingReview;
        }
        return null;
    }

    @Transactional
    public void delete(String id) {
        venueReviewRepository.deleteById(id);
    }
}