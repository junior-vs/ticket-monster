INSERT INTO catalog.media_type_catalog (code, description, enabled)
VALUES
    ('VIDEO', 'Promotional video', TRUE),
    ('AUDIO', 'Promotional audio', TRUE)
ON CONFLICT (code) DO NOTHING;

INSERT INTO catalog.event_category (id, description, created_at)
VALUES
    ('9eb551cd-0d79-4ad4-b98c-c2ee9f5faf01', 'Rock', '2026-01-10T10:00:00Z'),
    ('9eb551cd-0d79-4ad4-b98c-c2ee9f5faf02', 'Theater', '2026-01-10T10:05:00Z'),
    ('9eb551cd-0d79-4ad4-b98c-c2ee9f5faf03', 'Festival', '2026-01-10T10:10:00Z')
ON CONFLICT DO NOTHING;

INSERT INTO catalog.media_item (id, media_type_code, url, cached_file_name, fallback_applied, created_at)
VALUES
    ('5b8b446d-a3d7-4fb9-b280-f07f2e614201', 'IMAGE', 'https://cdn.ticketmonster.local/events/iron-festival.jpg', 'iron-festival.jpg', FALSE, '2026-01-11T09:00:00Z'),
    ('5b8b446d-a3d7-4fb9-b280-f07f2e614202', 'VIDEO', 'https://cdn.ticketmonster.local/events/stage-lights-teaser.mp4', 'stage-lights-teaser.mp4', FALSE, '2026-01-11T09:10:00Z'),
    ('5b8b446d-a3d7-4fb9-b280-f07f2e614203', 'IMAGE', 'https://cdn.ticketmonster.local/events/classics-night.jpg', 'not_available.jpg', TRUE, '2026-01-11T09:20:00Z')
ON CONFLICT DO NOTHING;

INSERT INTO catalog.event (id, name, description, event_category_id, media_item_id, status, created_at, updated_at, published_at)
VALUES
    (
        '7c84dd06-fbf5-4c25-9c82-49349e1e5301',
        'Iron Festival',
        'Large rock festival with two stages, premium hospitality, and exclusive fan experiences.',
        '9eb551cd-0d79-4ad4-b98c-c2ee9f5faf03',
        '5b8b446d-a3d7-4fb9-b280-f07f2e614201',
        'PUBLISHED',
        '2026-01-12T08:00:00Z',
        '2026-01-15T08:00:00Z',
        '2026-01-15T08:00:00Z'
    ),
    (
        '7c84dd06-fbf5-4c25-9c82-49349e1e5302',
        'Stage Lights',
        'Modern theater production with immersive lighting design and live music throughout the performance.',
        '9eb551cd-0d79-4ad4-b98c-c2ee9f5faf02',
        '5b8b446d-a3d7-4fb9-b280-f07f2e614202',
        'DRAFT',
        '2026-01-13T08:00:00Z',
        '2026-01-13T08:00:00Z',
        NULL
    ),
    (
        '7c84dd06-fbf5-4c25-9c82-49349e1e5303',
        'Classics Night',
        'Symphonic concert featuring chamber orchestra, guest soloists, and an archival retrospective program.',
        '9eb551cd-0d79-4ad4-b98c-c2ee9f5faf01',
        '5b8b446d-a3d7-4fb9-b280-f07f2e614203',
        'ARCHIVED',
        '2026-01-14T08:00:00Z',
        '2026-01-20T08:00:00Z',
        '2026-01-16T08:00:00Z'
    )
ON CONFLICT DO NOTHING;

INSERT INTO catalog.venue (id, name, description, address_line, city, state, postal_code, country, created_at)
VALUES
    (
        '42c6a4ac-33f0-4124-ba62-6b8e596cb101',
        'Arena Central',
        'Main arena prepared for high-volume concerts and premium sections.',
        '100 Grand Avenue',
        'Sao Paulo',
        'SP',
        '01000-000',
        'Brazil',
        '2026-01-12T10:00:00Z'
    ),
    (
        '42c6a4ac-33f0-4124-ba62-6b8e596cb102',
        'Teatro Aurora',
        'Indoor theater optimized for stage productions and acoustic performances.',
        '55 Liberty Street',
        'Campinas',
        'SP',
        '13000-000',
        'Brazil',
        '2026-01-12T10:05:00Z'
    )
ON CONFLICT DO NOTHING;

INSERT INTO catalog.section (id, venue_id, name, number_of_rows, row_capacity)
VALUES
    ('3d685ba6-1a68-4273-8c39-b2dd476d8201', '42c6a4ac-33f0-4124-ba62-6b8e596cb101', 'Premium Floor', 20, 30),
    ('3d685ba6-1a68-4273-8c39-b2dd476d8202', '42c6a4ac-33f0-4124-ba62-6b8e596cb101', 'North Stand', 25, 40),
    ('3d685ba6-1a68-4273-8c39-b2dd476d8203', '42c6a4ac-33f0-4124-ba62-6b8e596cb102', 'Orchestra', 15, 20),
    ('3d685ba6-1a68-4273-8c39-b2dd476d8204', '42c6a4ac-33f0-4124-ba62-6b8e596cb102', 'Balcony', 10, 18)
ON CONFLICT DO NOTHING;

INSERT INTO catalog.show (id, event_id, venue_id, created_at)
VALUES
    ('f4b6701a-3a97-4538-93ce-8ff089d61101', '7c84dd06-fbf5-4c25-9c82-49349e1e5301', '42c6a4ac-33f0-4124-ba62-6b8e596cb101', '2026-01-16T09:00:00Z'),
    ('f4b6701a-3a97-4538-93ce-8ff089d61102', '7c84dd06-fbf5-4c25-9c82-49349e1e5302', '42c6a4ac-33f0-4124-ba62-6b8e596cb102', '2026-01-16T09:05:00Z')
ON CONFLICT DO NOTHING;

INSERT INTO catalog.performance (id, show_id, performance_date, description)
VALUES
    ('c7e98b74-0d0b-4034-90cc-54ff4d4f5301', 'f4b6701a-3a97-4538-93ce-8ff089d61101', '2026-09-18T21:00:00Z', 'Opening night'),
    ('c7e98b74-0d0b-4034-90cc-54ff4d4f5302', 'f4b6701a-3a97-4538-93ce-8ff089d61101', '2026-09-19T21:00:00Z', 'Weekend encore'),
    ('c7e98b74-0d0b-4034-90cc-54ff4d4f5303', 'f4b6701a-3a97-4538-93ce-8ff089d61102', '2026-10-02T20:30:00Z', 'Preview session'),
    ('c7e98b74-0d0b-4034-90cc-54ff4d4f5304', 'f4b6701a-3a97-4538-93ce-8ff089d61102', '2026-10-03T20:30:00Z', 'Opening session')
ON CONFLICT DO NOTHING;
