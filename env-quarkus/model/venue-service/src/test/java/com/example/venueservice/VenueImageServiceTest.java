package com.example.venueservice;

import com.example.venueservice.entity.VenueImage;
import com.example.venueservice.repository.VenueImageRepository;
import com.example.venueservice.service.VenueImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class VenueImageServiceTest {

    @Mock
    private VenueImageRepository venueImageRepository;

    @InjectMocks
    private VenueImageService venueImageService;

    private VenueImage venueImage;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        venueImage = new VenueImage();
        venueImage.setId(UUID.randomUUID());
        venueImage.setVenueId(UUID.randomUUID());
        venueImage.setUrl("http://example.com/image.jpg");
        venueImage.setDescription("Sample Image");
        venueImage.setUploadedAt(LocalDateTime.now());
    }

    @Test
    public void testSaveVenueImage() {
        when(venueImageRepository.save(any(VenueImage.class))).thenReturn(venueImage);
        VenueImage savedImage = venueImageService.saveVenueImage(venueImage);
        assertNotNull(savedImage);
        assertEquals(venueImage.getUrl(), savedImage.getUrl());
        verify(venueImageRepository, times(1)).save(venueImage);
    }

    @Test
    public void testGetVenueImageById() {
        when(venueImageRepository.findById(any(UUID.class))).thenReturn(Optional.of(venueImage));
        VenueImage foundImage = venueImageService.getVenueImageById(venueImage.getId());
        assertNotNull(foundImage);
        assertEquals(venueImage.getId(), foundImage.getId());
        verify(venueImageRepository, times(1)).findById(venueImage.getId());
    }

    @Test
    public void testDeleteVenueImage() {
        doNothing().when(venueImageRepository).deleteById(any(UUID.class));
        venueImageService.deleteVenueImage(venueImage.getId());
        verify(venueImageRepository, times(1)).deleteById(venueImage.getId());
    }
}