package br.studio.ticketmonster.event;

import static io.restassured.RestAssured.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

@ExtendWith(MockitoExtension.class)
@QuarkusTest
public class EventServiceTest {

    private static final String REQUEST_JSON_PATH = "/test/response/create-request.json";
    private EventMapper eventMapper;
    private Logger logger = LoggerFactory.getLogger(EventMapperTest.class);
    private EventService eventService;
    private EventRepository eventRepository;

    @Inject
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        eventMapper = Mappers.getMapper(EventMapper.class);
        eventRepository = Mockito.mock(EventRepository.class);
        
        eventService = new EventService(eventMapper, eventRepository);

    }

    @Test
    void shouldCreateEventSuccessfully() {
        var testRequest = createRequest();
        var testEvent = createEntity();
        var  testResponse = new EventResponse(
            1L,
            "Rock in Rio",
            "Maior festival de música do Brasil",
            "Música",
            LocalDate.parse("2025-09-27"),
            LocalDate.parse("2025-09-30"),
            "Rio de Janeiro",
            "https://example.com/rockinrio.jpg"
        );

        when(eventRepository.persist(any(Event.class))).thenReturn(Uni.createFrom().item(testEvent));

        EventResponse result = eventService.createEvent(testRequest)
                .await()
                .indefinitely();

        logger.info(result.toString());

        assertEquals(testResponse.name(), result.name());
        assertEquals(testResponse.description(), result.description());
        assertEquals(testResponse.category(), result.category());
        assertEquals(testResponse.startDate(), result.startDate());
        assertEquals(testResponse.endDate(), result.endDate());
        assertEquals(testResponse.location(), result.location());
        assertEquals(testResponse.imageUrl(), result.imageUrl());

        verifyNoMoreInteractions(eventRepository);



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
