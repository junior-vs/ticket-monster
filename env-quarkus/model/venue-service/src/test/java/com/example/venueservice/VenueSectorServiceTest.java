package com.example.venueservice;

import com.example.venueservice.entity.VenueSector;
import com.example.venueservice.repository.VenueSectorRepository;
import com.example.venueservice.service.VenueSectorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class VenueSectorServiceTest {

    @Mock
    private VenueSectorRepository venueSectorRepository;

    @InjectMocks
    private VenueSectorService venueSectorService;

    private VenueSector venueSector;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        venueSector = new VenueSector();
        venueSector.setId(UUID.randomUUID());
        venueSector.setName("Main Hall");
        venueSector.setCapacity(200);
        venueSector.setLayout(new JSONObject().put("chairs", 100).put("tables", 20).toString());
    }

    @Test
    public void testCreateVenueSector() {
        when(venueSectorRepository.save(any(VenueSector.class))).thenReturn(venueSector);
        VenueSector created = venueSectorService.createVenueSector(venueSector);
        assertNotNull(created);
        assertEquals(venueSector.getName(), created.getName());
        verify(venueSectorRepository, times(1)).save(venueSector);
    }

    @Test
    public void testGetVenueSectorById() {
        when(venueSectorRepository.findById(any(UUID.class))).thenReturn(Optional.of(venueSector));
        VenueSector found = venueSectorService.getVenueSectorById(venueSector.getId());
        assertNotNull(found);
        assertEquals(venueSector.getName(), found.getName());
        verify(venueSectorRepository, times(1)).findById(venueSector.getId());
    }

    @Test
    public void testUpdateVenueSector() {
        when(venueSectorRepository.findById(any(UUID.class))).thenReturn(Optional.of(venueSector));
        when(venueSectorRepository.save(any(VenueSector.class))).thenReturn(venueSector);
        
        venueSector.setName("Updated Hall");
        VenueSector updated = venueSectorService.updateVenueSector(venueSector.getId(), venueSector);
        
        assertNotNull(updated);
        assertEquals("Updated Hall", updated.getName());
        verify(venueSectorRepository, times(1)).save(venueSector);
    }

    @Test
    public void testDeleteVenueSector() {
        doNothing().when(venueSectorRepository).deleteById(any(UUID.class));
        venueSectorService.deleteVenueSector(venueSector.getId());
        verify(venueSectorRepository, times(1)).deleteById(venueSector.getId());
    }
}