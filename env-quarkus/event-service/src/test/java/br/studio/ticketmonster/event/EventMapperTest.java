package br.studio.ticketmonster.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
public class EventMapperTest {

    private static final String REQUEST_JSON_PATH = "/test/response/create-request.json";

    private EventMapper eventMapper;

    private Logger logger = LoggerFactory.getLogger(EventMapperTest.class);


    @Inject
    ObjectMapper objectMapper;


    @BeforeEach
    void setUp() {
        eventMapper = Mappers.getMapper(EventMapper.class);
    }
    @Test
    void testToEntity() {

        EventRequest request = createRequest();
        logger.info(request.toString());
        // When
        Event entity = eventMapper.toEntity(request);

        logger.info(entity.toString());

        // Then
        assertNotNull(entity);
        assertEquals(request.name(), entity.getName());
        assertEquals(request.description(), entity.getDescription());
        assertEquals(request.category(), entity.getCategory());
        assertEquals(request.startDate(), entity.getStartDate());
        assertEquals(request.endDate(), entity.getEndDate());
        assertEquals(request.location(), entity.getLocation());
        assertEquals(request.imageUrl(), entity.getImageUrl());

    }

    @Test
    void testToResponse() {

        Event testEvent = createEntity();
        EventResponse response = eventMapper.toResponse(testEvent);

        logger.info(response.toString());


        // Then
        assertNotNull(response);
        assertEquals(testEvent.id, response.id());
        assertEquals(testEvent.getName(), response.name());
        assertEquals(testEvent.getDescription(), response.description());
        assertEquals(testEvent.getCategory(), response.category());
        assertEquals(testEvent.getStartDate(), response.startDate());
        assertEquals(testEvent.getEndDate(), response.endDate());
        assertEquals(testEvent.getLocation(), response.location());
        assertEquals(testEvent.getImageUrl(), response.imageUrl());

    }


    private EventRequest createRequest() {
        try {
            // Get resource as InputStream
            InputStream inputStream = getClass().getResourceAsStream(REQUEST_JSON_PATH);
            // Convert JSON to EventRequest
            return objectMapper.readValue(inputStream, EventRequest.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load test data from " + REQUEST_JSON_PATH, e);
        }
    }

    private Event createEntity() {
        return new Event(
                "Rock in Rio",
                "Maior festival de música do Brasil",
                "Música",
                LocalDate.parse("2025-09-27"),
                LocalDate.parse("2025-09-30"),
                "Rio de Janeiro",
                "https://example.com/rockinrio.jpg");
    }

}
