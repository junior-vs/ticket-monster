--liquibase formatted sql

--changeset vsjr:1.0.2-event-reference-restrict
--comment Protecao de integridade referencial da categoria de evento (RN04 / FR-004, US-CAT-09)
--comment Apenas os atributos necessarios a FK ON DELETE RESTRICT; o schema completo de
--comment evento pertence a US-CAT-08 e sera estendido em migration propria.
CREATE TABLE catalog.event (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_category_id UUID NOT NULL REFERENCES catalog.event_category(id) ON DELETE RESTRICT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_event_category ON catalog.event (event_category_id);
--rollback DROP TABLE catalog.event;
