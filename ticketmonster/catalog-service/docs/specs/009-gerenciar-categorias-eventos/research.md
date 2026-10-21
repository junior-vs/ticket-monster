# Research: Gerenciar Catálogo de Categorias de Eventos (US-CAT-09)

## 1. Domain Architecture & Layering

### Decision
Adotar **Clean Architecture (Hexagonal)** com separação estrita em 4 camadas internas no microsserviço `microservice-catalog`:
- `adapter-in`: REST Resources (JAX-RS / RESTEasy Reactive) + DTOs.
- `application`: Use Cases / Application Services (`CreateEventCategoryUseCase`, `UpdateEventCategoryUseCase`, `DeleteEventCategoryUseCase`, `ListEventCategoriesUseCase`).
- `domain`: Entidade `EventCategory` (pura/framework-agnostic com normalização `trim()`), Value Objects e Exceções de Domínio.
- `adapter-out`: Repositório Panache Reativo (`EventCategoryRepository`), Redis Cache Client (`CategoryCacheAdapter`).

### Rationale
Alinha-se com o **Princípio I da Constituição** e **ADR 01 / Seção 4.1 da Arquitetura de Referência**. Garante que a lógica de validação de negócios e tratamento de invariants (incluindo normalização de espaços nas bordas) permaneça agnóstica de frameworks e testável sem containers.

### Alternatives Considered
- *Anemic Domain Model com anotações JPA diretas na classe de domínio:* Rejeitado por violar a Constituição (Princípio I) e repetir o acoplamento do legado.

---

## 2. Persistência, Schema PostgreSQL & Normalização (FR-002a)

### Decision
- **Schema/Tabela**: `catalog.event_category`
- **Campos**:
  - `id` UUID PRIMARY KEY DEFAULT `gen_random_uuid()`
  - `description` VARCHAR(120) NOT NULL
  - `created_at` TIMESTAMPTZ NOT NULL DEFAULT `now()`
- **Constraints**:
  - `CONSTRAINT uq_event_category_description UNIQUE (description)` (enforces RN06)
  - `CONSTRAINT ck_event_category_description_trimmed CHECK (description = btrim(description))` (FR-002a: garante no banco que nenhuma string com espaços nas pontas seja gravada)
  - `CONSTRAINT ck_event_category_description_not_empty CHECK (description <> '')`
- **Normalização no Use Case**: A camada de aplicação executa `description.trim()` antes da validação e da persistência/busca.
- **Integridade Referencial**: A FK em `catalog.event.event_category_id` possui `ON DELETE RESTRICT` para garantir a nível de banco a proibição de remoção de categorias em uso (RN04 / `[NOVO]`).

### Rationale
UUID evita contenção e colisão entre microsserviços. A combinação do `btrim()` no use case com a constraint `CHECK (description = btrim(description))` no PostgreSQL previne condições de corrida e duplicidades semânticas por espaços em branco (FR-002a).

### Alternatives Considered
- *Constraint UNIQUE simples sem CHECK de btrim*: Rejeitado porque permitiria gravar `"Rock "` e `"Rock"` como registros distintos no PostgreSQL se a aplicação falhasse em aplicar trim.

---

## 3. Estratégia de Caching e Invalidação (Redis) (FR-007 / FR-007a)

### Decision
Implementar **Cache-Aside** no Redis via Quarkus Redis Client (`reactive-redis-client`):
- **Chave de Listagem**: `catalog:categories:list` (contrato formalizado em FR-007)
- **Invalidação Síncrona**: Em qualquer operação de gravação de sucesso (POST, PUT, DELETE), o serviço invalida síncronamente a chave `catalog:categories:list` (`reactiveRedisDataSource.key().del("catalog:categories:list")`) na mesma transação de escrita (FR-007a).
- **Sem TTL Fixo**: O cache não depende de expiração por tempo; é estritamente invalidado após mutações administrativas.

### Rationale
Atende aos requisitos funcionais **FR-007 / FR-007a** e ao orçamento de desempenho **PR-002** (leitura em <= 50ms p95). Evita que a interface pública exiba categorias obsoletas ou omitidas após alterações administrativas.

### Alternatives Considered
- *Chave catalog:categories:all com TTL fixo de 1 hora*: Rejeitado em favor do contrato padronizado `catalog:categories:list` com invalidação explícita por evento de escrita, conforme FR-007/FR-007a da spec.md.

---

## 4. Tratamento de Erros e Validação (RFC 7807)

### Decision
Mapear todas as exceções para o padrão RFC 7807 (Problem Details) via `ExceptionMapper` JAX-RS:
- `400 Bad Request`: Falhas sintáticas (descrição nula, em branco ou > 120 caracteres).
- `404 Not Found`: UUID de categoria inexistente em PUT ou DELETE.
- `409 Conflict`: Descrição duplicada (RN06) ou exclusão de categoria com eventos vinculados (`ON DELETE RESTRICT`).
- `401 Unauthorized` / `403 Forbidden`: Falhas de autenticação/autorização sem role `ROLE_ADMIN`.

### Rationale
Atende ao **Princípio II da Constituição**, **FR-005** e à **Seção 8 (Padrão de Erro RFC 7807)** da arquitetura.

---

## 5. Segurança & Autorização (Keycloak JWT RBAC)

### Decision
Usar `quarkus-oidc` em modo `bearer-only`:
- `GET /api/v1/event-categories`: Leitura pública (`permit`), extensão necessária para os filtros públicos do catálogo (US-CAT-02).
- `POST /api/v1/event-categories`: Protegido com `@RolesAllowed("ROLE_ADMIN")`.
- `PUT /api/v1/event-categories/{id}`: Protegido com `@RolesAllowed("ROLE_ADMIN")`.
- `DELETE /api/v1/event-categories/{id}`: Protegido com `@RolesAllowed("ROLE_ADMIN")`.

### Rationale
Garante a matriz de autorização da Seção 15.4 da Arquitetura de Solução com a extensão explícita declarada no item 1 das extensões da spec.
