package br.studio.ticketmonster.event;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.quarkus.test.InjectMock;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;

@QuarkusTest
@ExtendWith(MockitoExtension.class)
class EventResourceTest {

    @Mock
    EventService eventService;

    @InjectMock
    EventResource eventResource;

    private EventRequest eventRequest;
    private EventResponse eventResponse;

    @BeforeEach
    void setUp() {
      
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

      var  eventRequest =    new EventRequest(
            "Rock in Rio",
            "Maior festival de música do Brasil",            
            LocalDate.parse("2025-09-27T20:00:00"),
            LocalDate.parse("2025-09-30T23:59:59"),
            "Rio de Janeiro",
            1L
        );


        eventResponse = new EventResponse(
            1L,
            "Rock in Rio",
            "Maior festival de música do Brasil",
            LocalDate.parse("2025-09-27"),
            LocalDate.parse("2025-09-30"),
            "Rio de Janeiro",
            "uuid",
            LocalDateTime.now(),
            LocalDateTime.now(),
            null,
            null
        );
    }

    @Test
    void testCreateEvent() {
        // Arrange
       // when(eventService.createEvent(any(EventRequest.class))).thenReturn(Uni.createFrom().item(eventResponse));

        ArgumentMatcher<EventRequest> heroMatcher = argument -> {
           
            return argument.name().equals("Rock in Rio")
                && argument.description().equals("Maior festival de música do Brasil")
                && argument.startDate().equals(LocalDate.parse("2025-09-27"))
                && argument.endDate().equals(LocalDate.parse("2025-09-30"))
                && argument.location().equals("Rio de Janeiro")  // changed getLocation to location
                && argument.categoryId().equals(1L);  // changed getCategoryId to categoryId
        };
        
        when(this.eventService.createEvent(argThat(heroMatcher)))
			.thenReturn(Uni.createFrom().item(this.eventResponse));


        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .body(eventRequest)
            .when()
            .post("/events")
            .then()
            .statusCode(201)
            .header("Location", is("/events/1"))
            .body("id", is(1))
            .body("name", is("Rock in Rio"))
            .body("description", is("Maior festival de música do Brasil"))
            .body("startDate", is("2025-09-27"))
            .body("endDate", is("2025-09-30"))
            .body("location", is("Rio de Janeiro"))
            .body("uuid", is("uuid"));
    }


}
