-- Inserir categorias de evento


-- Ativa extensão para UUID aleatório
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Inserir categorias de evento

INSERT INTO event_category (id, description, created_at, updated_at)
VALUES
  (nextval('event_category_seq'), 'Show', NOW(), NOW()),
  (nextval('event_category_seq'), 'Teatro', NOW(), NOW()),
  (nextval('event_category_seq'), 'Esporte', NOW(), NOW());

-- Inserir eventos (ajuste os category_id conforme os IDs reais das categorias inseridas acima)

-- Inserir eventos

INSERT INTO event (id, uuid, name, description, start_date, end_date, location, category_id, created_at, updated_at)
VALUES
  (nextval('event_seq'), gen_random_uuid(), 'Rock in Rio', 'Festival de música', '2025-09-01T18:00:00', '2025-09-10T23:59:59', 'Rio de Janeiro', 1, NOW(), NOW()),
  (nextval('event_seq'), gen_random_uuid(), 'Peça Hamlet', 'Peça clássica de Shakespeare', '2025-10-05T20:00:00', '2025-10-05T22:00:00', 'Teatro Municipal', 2, NOW(), NOW());

-- Inserir itens de mídia (ajuste os event_id conforme os IDs reais dos eventos inseridos acima)

-- Inserir itens de mídia

INSERT INTO media_item (id, uuid, url, media_type, event_id, created_at, updated_at)
VALUES
  (nextval('media_item_seq'), gen_random_uuid(), 'https://exemplo.com/rockinrio.jpg', 'image', 1, NOW(), NOW()),
  (nextval('media_item_seq'), gen_random_uuid(), 'https://exemplo.com/hamlet.jpg', 'image', 2, NOW(), NOW());