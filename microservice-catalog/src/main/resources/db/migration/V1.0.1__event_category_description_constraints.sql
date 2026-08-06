ALTER TABLE catalog.event_category
    ADD CONSTRAINT ck_event_category_description_not_blank
        CHECK (btrim(description) <> '');

CREATE UNIQUE INDEX IF NOT EXISTS uq_event_category_description_normalized
    ON catalog.event_category (lower(btrim(description)));
