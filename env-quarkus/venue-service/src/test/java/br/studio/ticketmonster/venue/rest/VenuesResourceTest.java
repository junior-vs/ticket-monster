package br.studio.ticketmonster.venue.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import br.studio.ticketmonster.venue.model.VenuesListResponse;
import br.studio.ticketmonster.venue.model.VenuesRequest;
import br.studio.ticketmonster.venue.model.VenuesResponse;
import br.studio.ticketmonster.venue.service.VenuesService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.smallrye.mutiny.Uni;

@QuarkusTest
public class VenuesResourceTest {

    @InjectMock
    VenuesService venuesService;

    @BeforeEach
    public void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 8081;
    }

    @Test
    public void testGetAllVenues() {
        List<VenuesListResponse> venues = Collections.singletonList(new VenuesListResponse(UUID.randomUUID(), "Test Venue", "Test Description"));
        Mockito.when(venuesService.getAllVenues()).thenReturn(Uni.createFrom().item(venues));

        given()
          .when().get("/venues")
          .then()
             .statusCode(200)
             .body("size()", is(1))
             .body("[0].name", is("Test Venue"));
    }

    @Test
    public void testCreateVenue() {
        VenuesRequest venueRequest = new VenuesRequest(UUID.randomUUID(), "Test Venue", "Test Description");
        VenuesResponse venueResponse = new VenuesResponse(UUID.randomUUID(), "Test Venue", "Test Description");
        Mockito.when(venuesService.createVenue(any(VenuesRequest.class))).thenReturn(Uni.createFrom().item(venueResponse));

        given()
          .contentType("application/json")
          .body(venueRequest)
          .when().post("/venues")
          .then()
             .statusCode(201);
    }

    @Test
    public void testGetVenue() {
        UUID id = UUID.randomUUID();
        VenuesResponse venueResponse = new VenuesResponse(UUID.randomUUID(), "Test Venue", "Test Description");
        Mockito.when(venuesService.getVenue(any(UUID.class))).thenReturn(Uni.createFrom().item(venueResponse));

        given()
          .when().get("/venues/" + id)
          .then()
             .statusCode(200)
             .body("name", is("Test Venue"));
    }

    @Test
    public void testUpdateVenue() {
        UUID id = UUID.randomUUID();
        VenuesRequest venueRequest = new VenuesRequest(UUID.randomUUID(), "Test Venue", "Test Description");
        VenuesResponse venueResponse = new VenuesResponse(UUID.randomUUID(), "Updated Venue", "Test Description");
        Mockito.when(venuesService.updateVenue(any(UUID.class), any(VenuesRequest.class))).thenReturn(Uni.createFrom().item(venueResponse));

        given()
          .contentType("application/json")
          .body(venueRequest)
          .when().put("/venues/" + id)
          .then()
             .statusCode(200)
             .body("name", is("Updated Venue"));
    }

    @Test
    public void testDeleteVenue() {
        UUID id = UUID.randomUUID();
        Mockito.when(venuesService.deleteVenue(any(UUID.class))).thenReturn(Uni.createFrom().voidItem());

        given()
          .when().delete("/venues/" + id)
          .then()
             .statusCode(204);
    }
}
