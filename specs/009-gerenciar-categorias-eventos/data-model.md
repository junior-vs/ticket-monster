# Data Model: Gerenciar Catálogo de Categorias de Eventos (US-CAT-09)

## 1. Domain Entities & Value Objects

### `EventCategory` (Agregado Catalog — Entidade de Domínio)
Representa uma categoria de classificação temática para os eventos cadastrados no sistema (ex.: "Rock", "Teatro", "Futebol", "Orquestra").

| Campo | Tipo | Nulável | Modificável | Regras & Validadores |
|---|---|---|---|---|
| `id` | UUID | Não | Não | Gerado automaticamente (`UUID v4`). Chave primária. |
| `description` | String | Não | Sim | Obrigatoriamente entre 1 e 120 caracteres após `trim()`. Deve ser única globalmente no banco (RN06). |
| `createdAt` | Instant | Não | Não | Timestamp de criação gravado na persistência (`TIMESTAMPTZ`). |

---

## 2. Invariants & Business Rules (RNs)

- **RN06 (Categoria Única e Não Nula)**:
  - A descrição da categoria não pode ser nula, vazia ou composta apenas por espaços em branco.
  - A comparação para unicidade deve desconsiderar espaços sobressalentes nas pontas (`trim()`).
  - Duplicatas de descrição geram erro `409 Conflict` (Problem Details RFC 7807).
- **RN04 & Proteção de Integridade Referencial (`ON DELETE RESTRICT`)**:
  - Uma categoria de evento **NÃO pode ser excluída** se houver um ou mais eventos (`catalog.event`) associados a ela via `event_category_id`.
  - A exclusão de uma categoria sem eventos vinculados é permitida.
  - Tentativa de exclusão com eventos associados é interceptada e retorna `409 Conflict` detalhado via RFC 7807.

---

## 3. Schema DDL (PostgreSQL)

```sql
CREATE SCHEMA IF NOT EXISTS catalog;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ============================================================
-- Categoria de Evento (catalog.event_category)
-- ============================================================
CREATE TABLE catalog.event_category (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    description VARCHAR(120) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_event_category_description UNIQUE (description), -- RN06
    CONSTRAINT ck_event_category_description_not_empty CHECK (btrim(description) <> '')
);

-- ============================================================
-- Integridade Referencial na Tabela de Eventos (catalog.event)
-- ============================================================
-- A tabela catalog.event referencia event_category com ON DELETE RESTRICT
-- (garante protecao a nivel de banco contra remocao de categorias em uso)
--
-- ALTER TABLE catalog.event 
--   ADD CONSTRAINT fk_event_category 
--   FOREIGN KEY (event_category_id) REFERENCES catalog.event_category(id) ON DELETE RESTRICT;
```

---

## 4. Cache Representation (Redis)

- **Redis Key**: `catalog:categories:all`
- **Data Structure**: String (JSON Array serializado de DTOs `CategoryResponse`)
- **JSON Structure Example**:
```json
[
  {
    "id": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
    "description": "Futebol",
    "createdAt": "2026-07-25T14:30:00Z"
  },
  {
    "id": "b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22",
    "description": "Orquestra",
    "createdAt": "2026-07-25T14:35:00Z"
  },
  {
    "id": "c2eebc99-9c0b-4ef8-bb6d-6bb9bd380a33",
    "description": "Rock",
    "createdAt": "2026-07-25T14:20:00Z"
  }
]
```
- **Ordering**: Ordenado alfabeticamente por `description` ASC.
- **Cache Eviction Trigger**: Qualquer operação `POST`, `PUT` ou `DELETE` com sucesso em `/api/v1/event-categories` executa a chave `DEL catalog:categories:all`.
