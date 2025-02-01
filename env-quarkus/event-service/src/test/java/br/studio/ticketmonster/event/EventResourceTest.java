package br.studio.ticketmonster.event;

import static io.restassured.RestAssured.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;

@QuarkusTest
public class EventResourceTest {

    private static final String REQUEST_JSON_PATH = "/test/response/create-request.json";

    @Inject
    ObjectMapper objectMapper;

    @Test
    @DisplayName("Should return 201 Created when creating an event")
    public void createdTest() {
        EventService eventService = mock(EventService.class);
        UriInfo uriInfo = mock(UriInfo.class);
        UriBuilder uriBuilder = mock(UriBuilder.class);
        EventRequest request = createRequest();
        // EventResponse response = new EventResponse(1L, "Test Event",
        // LocalDateTime.now());
        URI location = URI.create("http://localhost:8081/events/1");

        when(eventService.createEvent(request)).thenReturn(createResponse());
        when(uriInfo.getAbsolutePathBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.path("1")).thenReturn(uriBuilder);
        when(uriBuilder.build()).thenReturn(location);

        given()
                .contentType("application/json")
                .body(request)
                .when()
                .post("/events")
                .then()
                .statusCode(201)
                .header("Location", location.toString());
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

    private Uni<EventResponse> createResponse() {
        return Uni.createFrom()
            .item(new EventResponse(1L, 
            "Test Event",
            "descriao" , 
            "categoria", 
            LocalDate.now(), 
            LocalDate.now(),
            null, 
            null));
    }


}
