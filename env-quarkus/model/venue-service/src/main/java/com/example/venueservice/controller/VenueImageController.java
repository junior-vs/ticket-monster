package com.example.venueservice.controller;

import com.example.venueservice.entity.VenueImage;
import com.example.venueservice.service.VenueImageService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/venue-images")
public class VenueImageController {

    @Autowired
    private VenueImageService venueImageService;

    @GetMapping
    public List<VenueImage> getAllImages() {
        return venueImageService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<VenueImage> getImageById(@PathVariable("id") String id) {
        return venueImageService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public VenueImage createImage(@RequestBody VenueImage venueImage) {
        return venueImageService.save(venueImage);
    }

    @PutMapping("/{id}")
    public ResponseEntity<VenueImage> updateImage(@PathVariable("id") String id, @RequestBody VenueImage venueImage) {
        return venueImageService.update(id, venueImage)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteImage(@PathVariable("id") String id) {
        if (venueImageService.delete(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}