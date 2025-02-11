package com.example.venueservice.controller;

import com.example.venueservice.entity.VenueSector;
import com.example.venueservice.service.VenueSectorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/venue-sectors")
public class VenueSectorController {

    @Autowired
    private VenueSectorService venueSectorService;

    @GetMapping
    public List<VenueSector> getAllVenueSectors() {
        return venueSectorService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<VenueSector> getVenueSectorById(@PathVariable("id") String id) {
        return venueSectorService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public VenueSector createVenueSector(@RequestBody VenueSector venueSector) {
        return venueSectorService.save(venueSector);
    }

    @PutMapping("/{id}")
    public ResponseEntity<VenueSector> updateVenueSector(@PathVariable("id") String id, @RequestBody VenueSector venueSector) {
        return venueSectorService.update(id, venueSector)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVenueSector(@PathVariable("id") String id) {
        if (venueSectorService.delete(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}