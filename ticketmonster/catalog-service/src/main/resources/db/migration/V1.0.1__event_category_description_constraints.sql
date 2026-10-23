--liquibase formatted sql

--changeset vsjr:1.0.1-create-event-category
--comment Cria a tabela de categorias de eventos (US-CAT-09, RN06, FR-002a)
CREATE SCHEMA IF NOT EXISTS catalog;

CREATE TABLE catalog.event_category (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    description VARCHAR(120) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_event_category_description UNIQUE (description),                                  -- RN06
    CONSTRAINT ck_event_category_description_trimmed CHECK (description = btrim(description)),      -- FR-002a
    CONSTRAINT ck_event_category_description_not_empty CHECK (description <> '')                    -- FR-002
);
--rollback DROP TABLE catalog.event_category;
