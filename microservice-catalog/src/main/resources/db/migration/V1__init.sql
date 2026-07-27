CREATE SCHEMA IF NOT EXISTS catalog;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ============================================================
-- Domínio de mídia (catálogo extensível — [ALTERA RN34])
-- ============================================================
CREATE TABLE catalog.media_type_catalog (
                                            code        VARCHAR(30)  PRIMARY KEY,      -- 'IMAGE', 'VIDEO', 'AUDIO'...
                                            description VARCHAR(120) NOT NULL,
                                            enabled     BOOLEAN      NOT NULL DEFAULT TRUE
);

INSERT INTO catalog.media_type_catalog (code, description, enabled)
VALUES ('IMAGE', 'Imagem promocional', TRUE); -- valor herdado do legado (RN34 as-is)

CREATE TABLE catalog.media_item (
                                    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                    media_type_code   VARCHAR(30) NOT NULL REFERENCES catalog.media_type_catalog(code),
                                    url               VARCHAR(2048) NOT NULL,
                                    cached_file_name  VARCHAR(255),
                                    fallback_applied  BOOLEAN NOT NULL DEFAULT FALSE, -- RN35
                                    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
                                    CONSTRAINT uq_media_item_url UNIQUE (url),                       -- RN37
                                    CONSTRAINT ck_media_item_url_scheme CHECK (url ~* '^https?://')  -- RN37
    );

-- ============================================================
-- Categoria de evento
-- ============================================================
CREATE TABLE catalog.event_category (
                                        id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                        description VARCHAR(120) NOT NULL,
                                        created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
                                        CONSTRAINT uq_event_category_description UNIQUE (description) -- RN06
);

-- ============================================================
-- Evento
-- ============================================================
CREATE TABLE catalog.event (
                               id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               name              VARCHAR(50) NOT NULL,
                               description       VARCHAR(1000) NOT NULL,
                               event_category_id UUID NOT NULL REFERENCES catalog.event_category(id) ON DELETE RESTRICT, -- RN04 + proteção [NOVO]
                               media_item_id     UUID NULL REFERENCES catalog.media_item(id) ON DELETE SET NULL,          -- RN05 (opcional)
                               status            VARCHAR(20) NOT NULL DEFAULT 'DRAFT',                                     -- [NOVO] ciclo de vida
                               created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
                               updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
                               published_at      TIMESTAMPTZ NULL,
                               CONSTRAINT uq_event_name UNIQUE (name),                                    -- RN01
                               CONSTRAINT ck_event_name_length CHECK (char_length(name) BETWEEN 5 AND 50),        -- RN02
                               CONSTRAINT ck_event_description_length CHECK (char_length(description) BETWEEN 20 AND 1000), -- RN03
                               CONSTRAINT ck_event_status CHECK (status IN ('DRAFT','PUBLISHED','ARCHIVED'))
);
CREATE INDEX ix_event_category ON catalog.event(event_category_id);
CREATE INDEX ix_event_status_published ON catalog.event(status) WHERE status = 'PUBLISHED'; -- US-CAT-01

-- ============================================================
-- Venue (com endereço embutido — equivalente ao @Embeddable Address do legado)
-- ============================================================
CREATE TABLE catalog.venue (
                               id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               name          VARCHAR(255) NOT NULL,
                               description   TEXT,
                               address_line  VARCHAR(255),
                               city          VARCHAR(120),
                               state         VARCHAR(120),
                               postal_code   VARCHAR(20),
                               country       VARCHAR(120),
                               created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
                               CONSTRAINT uq_venue_name UNIQUE (name),                 -- RN07
                               CONSTRAINT ck_venue_name_not_empty CHECK (btrim(name) <> '')  -- RN07
);

-- ============================================================
-- Seção física
-- ============================================================
CREATE TABLE catalog.section (
                                 id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                 venue_id        UUID NOT NULL REFERENCES catalog.venue(id) ON DELETE CASCADE,
                                 name            VARCHAR(120) NOT NULL,
                                 number_of_rows  INT NOT NULL,
                                 row_capacity    INT NOT NULL,
                                 capacity        INT GENERATED ALWAYS AS (number_of_rows * row_capacity) STORED, -- RN12
                                 CONSTRAINT uq_section_venue_name UNIQUE (venue_id, name),      -- RN11
                                 CONSTRAINT ck_section_rows_positive CHECK (number_of_rows > 0),
                                 CONSTRAINT ck_section_row_capacity_positive CHECK (row_capacity > 0)
);

-- ============================================================
-- Show (associação única Event + Venue)
-- ============================================================
CREATE TABLE catalog.show (
                              id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                              event_id   UUID NOT NULL REFERENCES catalog.event(id) ON DELETE CASCADE,
                              venue_id   UUID NOT NULL REFERENCES catalog.venue(id) ON DELETE RESTRICT,
                              created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                              CONSTRAINT uq_show_event_venue UNIQUE (event_id, venue_id) -- RN08
);
CREATE INDEX ix_show_venue ON catalog.show(venue_id); -- US-CAT-06

-- ============================================================
-- Performance (sessão de um Show)
-- ============================================================
CREATE TABLE catalog.performance (
                                     id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                     show_id           UUID NOT NULL REFERENCES catalog.show(id) ON DELETE CASCADE,
                                     performance_date  TIMESTAMPTZ NOT NULL,     -- RN09
                                     description       VARCHAR(255),
                                     CONSTRAINT uq_performance_show_date UNIQUE (show_id, performance_date) -- RN10
);
CREATE INDEX ix_performance_date ON catalog.performance(performance_date); -- US-CAT-07, RN31/RN32 (consumido por telemetry)