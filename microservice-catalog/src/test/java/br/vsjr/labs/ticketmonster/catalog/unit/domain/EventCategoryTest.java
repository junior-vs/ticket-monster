package br.vsjr.labs.ticketmonster.catalog.unit.domain;

import br.vsjr.labs.ticketmonster.catalog.domain.model.EventCategory;
import br.vsjr.labs.ticketmonster.catalog.domain.model.EventCategoryDescription;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EventCategoryTest {

    @Test
    @DisplayName("RN06 - should trim category description on creation")
    void shouldTrimDescriptionOnCreation() {
        var category = new EventCategory(new EventCategoryDescription("  Rock  "));
        assertEquals("Rock", category.description().value());
    }

    @Test
    @DisplayName("RN06 - should reject blank category description")
    void shouldRejectBlankDescription() {
        assertThrows(IllegalArgumentException.class, () -> new EventCategoryDescription("   "));
    }
}
