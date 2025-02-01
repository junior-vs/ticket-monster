package br.studio.ticketmonster.event;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EventTest {

    private Event event1;
    private Event event2;


      @BeforeEach
    void setUp() {

        event1 = createEntity();
        event2 = createEntity();
    }


@Test
    void equals_ShouldReturnTrue_WhenSameObject() {
        assertTrue(event1.equals(event1));
    }

    @Test
    void equals_ShouldReturnFalse_WhenNull() {
        assertFalse(event1.equals(null));
    }

    @Test
    void equals_ShouldReturnFalse_WhenDifferentClass() {
        assertFalse(event1.equals("not an event"));
    }

    @Test
    void equals_ShouldReturnFalse_WhenDifferentName() {
        Event differentEvent = new Event(
            "Different name",
            "Description",
            "Music",
            LocalDate.parse("2025-09-01"),
            LocalDate.parse("2025-09-30"),
            "Rio",
            "image.jpg"
        );
        assertFalse(event1.equals(differentEvent));
        assertNotEquals(event1.hashCode(), differentEvent.hashCode());
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
