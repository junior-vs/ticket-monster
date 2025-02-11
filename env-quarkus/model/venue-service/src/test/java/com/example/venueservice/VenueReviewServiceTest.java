package com.example.venueservice;

import com.example.venueservice.entity.VenueReview;
import com.example.venueservice.repository.VenueReviewRepository;
import com.example.venueservice.service.VenueReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class VenueReviewServiceTest {

    @Mock
    private VenueReviewRepository venueReviewRepository;

    @InjectMocks
    private VenueReviewService venueReviewService;

    private VenueReview venueReview;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        venueReview = new VenueReview();
        venueReview.setId(UUID.randomUUID());
        venueReview.setVenueId(UUID.randomUUID());
        venueReview.setUserId(UUID.randomUUID());
        venueReview.setRating(5);
        venueReview.setComment("Great venue!");
    }

    @Test
    public void testCreateReview() {
        when(venueReviewRepository.save(any(VenueReview.class))).thenReturn(venueReview);
        VenueReview createdReview = venueReviewService.createReview(venueReview);
        assertNotNull(createdReview);
        assertEquals(venueReview.getComment(), createdReview.getComment());
        verify(venueReviewRepository, times(1)).save(venueReview);
    }

    @Test
    public void testGetReviewById() {
        when(venueReviewRepository.findById(any(UUID.class))).thenReturn(Optional.of(venueReview));
        VenueReview foundReview = venueReviewService.getReviewById(venueReview.getId());
        assertNotNull(foundReview);
        assertEquals(venueReview.getId(), foundReview.getId());
        verify(venueReviewRepository, times(1)).findById(venueReview.getId());
    }

    @Test
    public void testDeleteReview() {
        doNothing().when(venueReviewRepository).deleteById(any(UUID.class));
        venueReviewService.deleteReview(venueReview.getId());
        verify(venueReviewRepository, times(1)).deleteById(venueReview.getId());
    }
}