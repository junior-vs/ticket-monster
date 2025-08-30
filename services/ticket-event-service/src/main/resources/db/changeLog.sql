--liquibase formatted sql

--changeset junior:1
--comment: Create initial databse
CREATE DATABASE ticket_event
  WITH OWNER = quarkus
  ENCODING = 'UTF8'
  CONNECTION LIMIT = -1;

--changeset junior:1
--comment: Create initial  event_category
CREATE TABLE event_category (
                                id BIGSERIAL PRIMARY KEY,
                                description VARCHAR(255) NOT NULL,
                                created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

--changeset junior:1
--comment: Create initial  event
CREATE TABLE event (
                       id BIGSERIAL PRIMARY KEY,
                       uuid UUID NOT NULL UNIQUE,
                       name VARCHAR(255) NOT NULL,
                       description TEXT,
                       start_date TIMESTAMP NOT NULL,
                       end_date TIMESTAMP NOT NULL,
                       location VARCHAR(255),
                       category_id BIGINT NOT NULL REFERENCES event_category(id),
                       created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                       updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

--changeset junior:1
--comment: Create initial  media_item
CREATE TABLE media_item (
                            id UUID PRIMARY KEY,
                            uuid UUID NOT NULL UNIQUE,
                            url VARCHAR(512) NOT NULL,
                            media_type VARCHAR(32) NOT NULL,
                            event_id UUID NOT NULL REFERENCES event(uuid),
                            created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

--changeset junior:1
CREATE INDEX idx_event_category_id ON event(category_id);
--changeset junior:1
CREATE INDEX idx_media_item_event_id ON media_item(event_id);