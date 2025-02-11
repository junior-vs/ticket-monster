package com.example.venueservice.service;

import com.example.venueservice.entity.VenueImage;
import com.example.venueservice.repository.VenueImageRepository;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class VenueImageService {

    @Inject
    VenueImageRepository venueImageRepository;

    public List<VenueImage> findAll() {
        return venueImageRepository.findAll().list();
    }

    public Optional<VenueImage> findById(String id) {
        return venueImageRepository.findByIdOptional(id);
    }

    @Transactional
    public VenueImage create(VenueImage venueImage) {
        venueImageRepository.persist(venueImage);
        return venueImage;
    }

    @Transactional
    public VenueImage update(VenueImage venueImage) {
        return venueImageRepository.getEntityManager().merge(venueImage);
    }

    @Transactional
    public void delete(String id) {
        venueImageRepository.deleteById(id);
    }
}