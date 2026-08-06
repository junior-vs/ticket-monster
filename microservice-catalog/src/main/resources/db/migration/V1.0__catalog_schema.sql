CREATE SCHEMA IF NOT EXISTS catalog;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE catalog.media_type_catalog (
    code VARCHAR(30) PRIMARY KEY,
    description VARCHAR(120) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO catalog.media_type_catalog (code, description, enabled)
VALUES ('IMAGE', 'Promotional image', TRUE);

CREATE TABLE catalog.media_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    media_type_code VARCHAR(30) NOT NULL REFERENCES catalog.media_type_catalog(code),
    url VARCHAR(2048) NOT NULL,
    cached_file_name VARCHAR(255),
    fallback_applied BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_media_item_url UNIQUE (url),
    CONSTRAINT ck_media_item_url_scheme CHECK (url ~* '^https?://')
);

CREATE TABLE catalog.event_category (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    description VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_event_category_description UNIQUE (description)
);

CREATE TABLE catalog.event (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    event_category_id UUID NOT NULL REFERENCES catalog.event_category(id) ON DELETE RESTRICT,
    media_item_id UUID NULL REFERENCES catalog.media_item(id) ON DELETE SET NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ NULL,
    CONSTRAINT uq_event_name UNIQUE (name),
    CONSTRAINT ck_event_name_length CHECK (char_length(name) BETWEEN 5 AND 50),
    CONSTRAINT ck_event_description_length CHECK (char_length(description) BETWEEN 20 AND 1000),
    CONSTRAINT ck_event_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED'))
);

CREATE INDEX ix_event_category ON catalog.event(event_category_id);
CREATE INDEX ix_event_status_published ON catalog.event(status) WHERE status = 'PUBLISHED';

CREATE TABLE catalog.venue (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    address_line VARCHAR(255),
    city VARCHAR(120),
    state VARCHAR(120),
    postal_code VARCHAR(20),
    country VARCHAR(120),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_venue_name UNIQUE (name),
    CONSTRAINT ck_venue_name_not_empty CHECK (btrim(name) <> '')
);

CREATE TABLE catalog.section (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    venue_id UUID NOT NULL REFERENCES catalog.venue(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    number_of_rows INT NOT NULL,
    row_capacity INT NOT NULL,
    capacity INT GENERATED ALWAYS AS (number_of_rows * row_capacity) STORED,
    CONSTRAINT uq_section_venue_name UNIQUE (venue_id, name),
    CONSTRAINT ck_section_rows_positive CHECK (number_of_rows > 0),
    CONSTRAINT ck_section_row_capacity_positive CHECK (row_capacity > 0)
);

CREATE TABLE catalog.show (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL REFERENCES catalog.event(id) ON DELETE CASCADE,
    venue_id UUID NOT NULL REFERENCES catalog.venue(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_show_event_venue UNIQUE (event_id, venue_id)
);

CREATE INDEX ix_show_venue ON catalog.show(venue_id);

CREATE TABLE catalog.performance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    show_id UUID NOT NULL REFERENCES catalog.show(id) ON DELETE CASCADE,
    performance_date TIMESTAMPTZ NOT NULL,
    description VARCHAR(255),
    CONSTRAINT uq_performance_show_date UNIQUE (show_id, performance_date)
);

CREATE INDEX ix_performance_date ON catalog.performance(performance_date);
