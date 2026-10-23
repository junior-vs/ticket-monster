package br.vsjr.labs.ticketmonster.catalog.unit.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.vsjr.labs.ticketmonster.catalog.domain.exception.EventCategoryInvalidDescriptionException;
import br.vsjr.labs.ticketmonster.catalog.domain.model.EventCategory;
import br.vsjr.labs.ticketmonster.catalog.domain.model.EventCategoryDescription;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testes unitarios das invariantes do agregado de dominio {@link EventCategory}
 * e do Value Object {@link EventCategoryDescription}.
 *
 * <p>Nucleo puro - executado sem infraestrutura (AGENTS.md secao 10).
 */
class EventCategoryTest {

    @Test
    @DisplayName("FR-002a - a descricao e normalizada com trim das extremidades")
    void shouldTrimDescription() {
        assertEquals("Orquestra", EventCategoryDescription.of("  Orquestra  ").value());
    }

    @Test
    @DisplayName("FR-002 - descricao nula e rejeitada")
    void shouldRejectNullDescription() {
        assertThrows(EventCategoryInvalidDescriptionException.class, () -> EventCategoryDescription.of(null));
    }

    @Test
    @DisplayName("FR-002 - descricao vazia ou composta apenas por espacos e rejeitada")
    void shouldRejectBlankDescription() {
        assertThrows(EventCategoryInvalidDescriptionException.class, () -> EventCategoryDescription.of(""));
        assertThrows(EventCategoryInvalidDescriptionException.class, () -> EventCategoryDescription.of("   "));
    }

    @Test
    @DisplayName("FR-003 - descricao com ate 120 caracteres e aceita")
    void shouldAcceptDescriptionAtLimit() {
        String atLimit = "a".repeat(EventCategoryDescription.MAX_LENGTH);
        assertEquals(EventCategoryDescription.MAX_LENGTH, EventCategoryDescription.of(atLimit).value().length());
    }

    @Test
    @DisplayName("FR-003 - descricao acima de 120 caracteres e rejeitada")
    void shouldRejectDescriptionAboveLimit() {
        String overLimit = "a".repeat(EventCategoryDescription.MAX_LENGTH + 1);
        assertThrows(EventCategoryInvalidDescriptionException.class, () -> EventCategoryDescription.of(overLimit));
    }

    @Test
    @DisplayName("FR-003 / FR-002a - o limite de tamanho e avaliado apos a normalizacao")
    void shouldValidateLengthAfterTrim() {
        String padded = "  " + "a".repeat(EventCategoryDescription.MAX_LENGTH) + "  ";
        assertEquals(EventCategoryDescription.MAX_LENGTH, EventCategoryDescription.of(padded).value().length());
    }

    @Test
    @DisplayName("RN06 - a criacao gera um identificador UUID e o timestamp de criacao")
    void shouldGenerateIdentityOnCreate() {
        EventCategory category = EventCategory.create(EventCategoryDescription.of("Orquestra"));

        assertNotNull(category.id());
        assertNotNull(category.createdAt());
        assertEquals("Orquestra", category.description().value());
    }

    @Test
    @DisplayName("RN06 - duas categorias com a mesma descricao possuem identificadores distintos")
    void shouldAssignDistinctIdentifiersForSameDescription() {
        EventCategory first = EventCategory.create(EventCategoryDescription.of("Rock"));
        EventCategory second = EventCategory.create(EventCategoryDescription.of("Rock"));

        assertNotEquals(first.id(), second.id());
    }

    @Test
    @DisplayName("FR-001 - a descricao de uma categoria existente pode ser alterada")
    void shouldChangeDescription() {
        EventCategory category = EventCategory.create(EventCategoryDescription.of("Orquestra"));

        category.changeDescription(EventCategoryDescription.of("Orquestra Sinfonica"));

        assertEquals("Orquestra Sinfonica", category.description().value());
    }
}
