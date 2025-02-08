
-- Drop table
-- DROP TABLE public.eventcategory;
CREATE TABLE public.eventcategory (
    createdat timestamp(6) NULL,
    id int8 NOT NULL,
    updatedat timestamp(6) NULL,
    description varchar(255) NOT NULL,
    name varchar(255) NOT NULL,
    CONSTRAINT eventcategory_name_key UNIQUE (name),
    CONSTRAINT eventcategory_pkey PRIMARY KEY (id)
);
-- public."event" definition
-- Drop table
-- DROP TABLE public."event";
CREATE TABLE public."event" (
    enddate date NULL,
    startdate date NULL,
    category_id int8 NOT NULL,
    createdat timestamp(6) NULL,
    id int8 NOT NULL,
    updatedat timestamp(6) NULL,
    "uuid" uuid NULL,
    description varchar(255) NULL,
    "location" varchar(255) NULL,
    "name" varchar(255) NULL,
    CONSTRAINT event_pkey PRIMARY KEY (id),
    CONSTRAINT fk8csjtmgirbl21kpwsxo6x66nh FOREIGN KEY (category_id) REFERENCES public.eventcategory(id)
);
-- public.mediaitem definition
-- Drop table
-- DROP TABLE public.mediaitem;
CREATE TABLE public.mediaitem (
    createdat timestamp(6) NULL,
    event_id int8 NOT NULL,
    id int8 NOT NULL,
    updatedat timestamp(6) NULL,
    description varchar(255) NULL,
    mediatype varchar(255) NULL,
    url varchar(255) NOT NULL,
    CONSTRAINT mediaitem_mediatype_check CHECK (((mediatype)::text = ANY ((ARRAY['IMAGE'::character varying, 'VIDEO'::character varying, 'AUDIO'::character varying, 'TEXT'::character varying])::text[]))),
    CONSTRAINT mediaitem_pkey PRIMARY KEY (id),
    CONSTRAINT mediaitem_url_key UNIQUE (url),
    CONSTRAINT fkhjej91fsiuwogei3safeadntw FOREIGN KEY (event_id) REFERENCES public."event"(id)
);

-- Insert event categories
INSERT INTO public.eventcategory (id, name, description, createdat, updatedat) VALUES
(1, 'Show Musical', 'Categoria de Shows Musicais', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'Exposição de Arte', 'Categoria de Exposições de Arte', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'Festival Gastronômico', 'Categoria de Festivais Gastronômicos', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 'Conferência', 'Categoria de Conferências', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 'Workshop', 'Categoria de Workshops', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert events
INSERT INTO public.event (id, name, description, location, startdate, enddate, category_id, createdat, updatedat, uuid) VALUES
(1, 'Festival de Jazz', 'Festival internacional de jazz com artistas renomados', 'Teatro Municipal', '2025-03-15', '2025-03-17', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, gen_random_uuid()),
(2, 'Arte Moderna', 'Exposição de arte moderna contemporânea', 'Museu de Arte', '2025-04-01', '2025-04-30', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, gen_random_uuid()),
(3, 'Sabores do Mundo', 'Festival com pratos típicos de diversos países', 'Centro de Convenções', '2025-05-10', '2025-05-12', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, gen_random_uuid()),
(4, 'Tech Summit 2025', 'Conferência sobre tecnologias emergentes', 'Centro Empresarial', '2025-06-20', '2025-06-22', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, gen_random_uuid()),
(5, 'Workshop de Fotografia', 'Aprenda técnicas avançadas de fotografia', 'Estúdio Central', '2025-07-05', '2025-07-05', 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, gen_random_uuid());

-- Insert media items
INSERT INTO public.mediaitem (id, description, mediatype, url, event_id, createdat, updatedat) VALUES
(1, 'Cartaz do Festival de Jazz', 'IMAGE', 'https://eventos.com/jazz-festival-poster.jpg', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'Vídeo promocional do Festival de Jazz', 'VIDEO', 'https://eventos.com/jazz-festival-promo.mp4', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'Catálogo da exposição', 'TEXT', 'https://eventos.com/arte-moderna-catalogo.pdf', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 'Fotos da exposição', 'IMAGE', 'https://eventos.com/arte-moderna-fotos.jpg', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 'Áudio guia do festival gastronômico', 'AUDIO', 'https://eventos.com/sabores-guia.mp3', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, 'Apresentação da conferência', 'TEXT', 'https://eventos.com/tech-summit-slides.pdf', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(7, 'Material do workshop', 'TEXT', 'https://eventos.com/fotografia-material.pdf', 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);