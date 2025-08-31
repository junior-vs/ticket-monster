-- Criar o schema 'event_service'
CREATE SCHEMA IF NOT EXISTS event_service;

-- Definir o schema como padrão para as próximas instruções
SET search_path TO event_service;

-- Tabela para 'EventCategory'
-- Representa as categorias de eventos (ex: Música, Comédia)
CREATE TABLE IF NOT EXISTS event_category (
    id BIGSERIAL PRIMARY KEY,
    description VARCHAR(255) UNIQUE NOT NULL
);

-- Tabela para 'Event'
-- Entidade central do catálogo, representa um evento
CREATE TABLE IF NOT EXISTS event (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL,
    description TEXT NOT NULL,
    major BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 1,
    category_id BIGINT NOT NULL,
    FOREIGN KEY (category_id) REFERENCES event_category (id)
);

-- Tabela para 'Venue'
-- Representa os locais físicos dos eventos
CREATE TABLE IF NOT EXISTS venue (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL,
    description TEXT,
    address TEXT,
    capacity INT,
    version BIGINT NOT NULL DEFAULT 1
);

-- Tabela para 'MediaItem'
-- Armazena as URLs das mídias (imagens, vídeos)
CREATE TABLE IF NOT EXISTS media_item (
    id BIGSERIAL PRIMARY KEY,
    media_url VARCHAR(2048) UNIQUE NOT NULL,
    media_type VARCHAR(50) NOT NULL,
    event_id BIGINT,
    venue_id BIGINT,
    FOREIGN KEY (event_id) REFERENCES event (id) ON DELETE CASCADE,
    FOREIGN KEY (venue_id) REFERENCES venue (id) ON DELETE CASCADE
);


-- Tabela para 'Show'
-- Conecta um evento a um local
CREATE TABLE IF NOT EXISTS show_event (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL,
    venue_id BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    FOREIGN KEY (event_id) REFERENCES event (id) ON DELETE CASCADE,
    FOREIGN KEY (venue_id) REFERENCES venue (id) ON DELETE CASCADE
);

-- Tabela para 'Performance'
-- Define as datas e horários específicos de um show
CREATE TABLE IF NOT EXISTS performance (
    id BIGSERIAL PRIMARY KEY,
    show_id BIGINT NOT NULL,
    date TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT unique_show_date UNIQUE (show_id, date),
    FOREIGN KEY (show_id) REFERENCES show (id) ON DELETE CASCADE
);