package com.example.venueservice.controller;

import com.example.venueservice.entity.VenueReview;
import com.example.venueservice.service.VenueReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class VenueReviewController {

    @Autowired
    private VenueReviewService venueReviewService;

    @GetMapping
    public List<VenueReview> getAllReviews() {
        return venueReviewService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<VenueReview> getReviewById(@PathVariable("id") String id) {
        return venueReviewService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public VenueReview createReview(@RequestBody VenueReview venueReview) {
        return venueReviewService.save(venueReview);
    }

    @PutMapping("/{id}")
    public ResponseEntity<VenueReview> updateReview(@PathVariable("id") String id, @RequestBody VenueReview venueReview) {
        return venueReviewService.update(id, venueReview)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable("id") String id) {
        if (venueReviewService.delete(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}