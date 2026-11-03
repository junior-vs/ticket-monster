--liquibase formatted sql

--changeset vsjr:1.0.3-catalog-dev-seed context:dev,des
--comment Carga inicial de dados para ambiente local (DEV) das tabelas existentes (catalog.event_category e catalog.event)

INSERT INTO catalog.event_category (id, description, created_at)
VALUES
    ('9eb551cd-0d79-4ad4-b98c-c2ee9f5faf01', 'Concert', '2026-01-10T10:00:00Z'),
    ('9eb551cd-0d79-4ad4-b98c-c2ee9f5faf02', 'Theatre', '2026-01-10T10:05:00Z'),
    ('9eb551cd-0d79-4ad4-b98c-c2ee9f5faf03', 'Musical', '2026-01-10T10:10:00Z'),
    ('9eb551cd-0d79-4ad4-b98c-c2ee9f5faf04', 'Sporting', '2026-01-10T10:15:00Z'),
    ('9eb551cd-0d79-4ad4-b98c-c2ee9f5faf05', 'Comedy', '2026-01-10T10:20:00Z')
ON CONFLICT DO NOTHING;

INSERT INTO catalog.event (id, event_category_id, created_at)
VALUES
    ('7c84dd06-fbf5-4c25-9c82-49349e1e5301', '9eb551cd-0d79-4ad4-b98c-c2ee9f5faf01', '2026-01-11T10:00:00Z'),
    ('7c84dd06-fbf5-4c25-9c82-49349e1e5302', '9eb551cd-0d79-4ad4-b98c-c2ee9f5faf02', '2026-01-11T10:05:00Z'),
    ('7c84dd06-fbf5-4c25-9c82-49349e1e5303', '9eb551cd-0d79-4ad4-b98c-c2ee9f5faf03', '2026-01-11T10:10:00Z'),
    ('7c84dd06-fbf5-4c25-9c82-49349e1e5304', '9eb551cd-0d79-4ad4-b98c-c2ee9f5faf04', '2026-01-11T10:15:00Z'),
    ('7c84dd06-fbf5-4c25-9c82-49349e1e5305', '9eb551cd-0d79-4ad4-b98c-c2ee9f5faf05', '2026-01-11T10:20:00Z')
ON CONFLICT DO NOTHING;

--rollback DELETE FROM catalog.event WHERE id IN ('7c84dd06-fbf5-4c25-9c82-49349e1e5301', '7c84dd06-fbf5-4c25-9c82-49349e1e5302', '7c84dd06-fbf5-4c25-9c82-49349e1e5303', '7c84dd06-fbf5-4c25-9c82-49349e1e5304', '7c84dd06-fbf5-4c25-9c82-49349e1e5305');
--rollback DELETE FROM catalog.event_category WHERE id IN ('9eb551cd-0d79-4ad4-b98c-c2ee9f5faf01', '9eb551cd-0d79-4ad4-b98c-c2ee9f5faf02', '9eb551cd-0d79-4ad4-b98c-c2ee9f5faf03', '9eb551cd-0d79-4ad4-b98c-c2ee9f5faf04', '9eb551cd-0d79-4ad4-b98c-c2ee9f5faf05');
