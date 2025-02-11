package com.example.venueservice.repository;

import com.example.venueservice.entity.VenueSector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VenueSectorRepository extends JpaRepository<VenueSector, UUID> {
}