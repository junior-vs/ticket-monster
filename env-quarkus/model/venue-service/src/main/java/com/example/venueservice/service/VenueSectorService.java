package com.example.venueservice.service;

import com.example.venueservice.entity.VenueSector;
import com.example.venueservice.repository.VenueSectorRepository;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

public class VenueSectorService {

    @Inject
    VenueSectorRepository venueSectorRepository;

    public List<VenueSector> findAll() {
        return venueSectorRepository.findAll().list();
    }

    public Optional<VenueSector> findById(String id) {
        return venueSectorRepository.findByIdOptional(id);
    }

    @Transactional
    public VenueSector create(VenueSector venueSector) {
        venueSectorRepository.persist(venueSector);
        return venueSector;
    }

    @Transactional
    public VenueSector update(VenueSector venueSector) {
        return venueSectorRepository.getReferenceById(venueSector.getId());
    }

    @Transactional
    public void delete(String id) {
        venueSectorRepository.deleteById(id);
    }
}