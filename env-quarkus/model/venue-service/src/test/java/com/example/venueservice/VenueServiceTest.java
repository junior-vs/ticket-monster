package com.example.venueservice;

import com.example.venueservice.entity.Venue;
import com.example.venueservice.repository.VenueRepository;
import com.example.venueservice.service.VenueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class VenueServiceTest {

    @Mock
    private VenueRepository venueRepository;

    @InjectMocks
    private VenueService venueService;

    private Venue venue;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        venue = new Venue();
        venue.setId(UUID.randomUUID());
        venue.setName("Test Venue");
        venue.setAddress("123 Test St");
        venue.setCity("Test City");
        venue.setState("Test State");
        venue.setCountry("Test Country");
        venue.setPostalCode("12345");
        venue.setCapacity(100);
        venue.setVenueType("auditório");
    }

    @Test
    public void testCreateVenue() {
        when(venueRepository.save(any(Venue.class))).thenReturn(venue);
        Venue createdVenue = venueService.createVenue(venue);
        assertNotNull(createdVenue);
        assertEquals("Test Venue", createdVenue.getName());
        verify(venueRepository, times(1)).save(venue);
    }

    @Test
    public void testGetVenueById() {
        when(venueRepository.findById(any(UUID.class))).thenReturn(Optional.of(venue));
        Venue foundVenue = venueService.getVenueById(venue.getId());
        assertNotNull(foundVenue);
        assertEquals("Test Venue", foundVenue.getName());
    }

    @Test
    public void testUpdateVenue() {
        when(venueRepository.findById(any(UUID.class))).thenReturn(Optional.of(venue));
        venue.setName("Updated Venue");
        when(venueRepository.save(any(Venue.class))).thenReturn(venue);
        Venue updatedVenue = venueService.updateVenue(venue.getId(), venue);
        assertEquals("Updated Venue", updatedVenue.getName());
    }

    @Test
    public void testDeleteVenue() {
        when(venueRepository.findById(any(UUID.class))).thenReturn(Optional.of(venue));
        venueService.deleteVenue(venue.getId());
        verify(venueRepository, times(1)).delete(venue);
    }
}