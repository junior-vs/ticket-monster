# Research: Gerenciar Catálogo de Categorias de Eventos (US-CAT-09)

## 1. Domain Architecture & Layering

### Decision
Adotar **Clean Architecture (Hexagonal)** com separação estrita em 4 camadas internas no microsserviço `microservice-catalog`:
- `adapter-in`: REST Resources (JAX-RS / RESTEasy Reactive) + DTOs.
- `application`: Use Cases / Application Services (`CreateEventCategoryUseCase`, `UpdateEventCategoryUseCase`, `DeleteEventCategoryUseCase`, `ListEventCategoriesUseCase`).
- `domain`: Entidade `EventCategory` (pura/framework-agnostic), Value Objects e Exceções de Domínio.
- `adapter-out`: Repositório Panache Reativo (`EventCategoryRepository`), Redis Cache Client (`CategoryCacheAdapter`).

### Rationale
Alinha-se diretamente com o **Princípio I da Constituição** e **ADR 01 / Seção 4.1 da Arquitetura de Referência**. Garante que a lógica de validação de negócios e tratamento de invariants permaneça agnóstica de frameworks e facilmente testável sem subir containers ou dependências externas.

### Alternatives Considered
- *Anemic Domain Model com anotações JPA diretas na classe de domínio:* Rejeitado por violar a Constituição (Princípio I) e repetir o acoplamento do sistema legado.

---

## 2. Persistência e Schema PostgreSQL

### Decision
- **Schema/Tabela**: `catalog.event_category`
- **Campos**:
  - `id` UUID PRIMARY KEY DEFAULT `gen_random_uuid()`
  - `description` VARCHAR(120) NOT NULL
  - `created_at` TIMESTAMPTZ NOT NULL DEFAULT `now()`
- **Constraints**:
  - `CONSTRAINT uq_event_category_description UNIQUE (description)` (enforces RN06)
  - `CONSTRAINT ck_event_category_description_not_empty CHECK (btrim(description) <> '')`
- **Mapeamento Orquestração de Exclusão**: A FK em `catalog.event.event_category_id` possui `ON DELETE RESTRICT` para garantir que nenhuma categoria com eventos vinculados possa ser excluída diretamente no banco de dados.

### Rationale
UUID evita contenção e colisão de sequências entre microsserviços. A constraint de unicidade no PostgreSQL garante a integridade mesmo sob condições de concorrência massiva na inserção/edição.

### Alternatives Considered
- *BIGSERIAL autoincremento*: Rejeitado de acordo com a Seção 9.1 da arquitetura (UUIDs são padrão em toda a solução modernizada para integração distribuída).

---

## 3. Estratégia de Caching e Invalidação (Redis)

### Decision
Implementar **Cache-Aside** no Redis via Quarkus Redis Client (`reactive-redis-client`):
- **Chave de Listagem**: `catalog:categories:all`
- **TTL**: 3600 segundos (1 hora).
- **Invalidação**: Em qualquer operação de gravação de sucesso (criação POST, atualização PUT, exclusão DELETE), o serviço invalida de forma reativa a chave `catalog:categories:all` (`reactiveRedisDataSource.key().del("catalog:categories:all")`).

### Rationale
Atende ao requisito funcional **FR-007** e aos orçamentos de desempenho **PR-002** (leitura em <= 50ms p95). Como a alteração de categorias de eventos é infrequente e a leitura pública é intensiva (*read-heavy*), a invalidação proativa no write mantém a consistência final imediata para o canal de vendas.

### Alternatives Considered
- *Write-Through Cache*: Desnecessário para este cenário, pois a fonte da verdade relacional é o PostgreSQL e o volume de categorias é pequeno o suficiente para cachear a coleção listada inteira.

---

## 4. Tratamento de Erros e Validação (RFC 7807)

### Decision
Mapear todas as exceções da aplicação/domínio para respostas HTTP no padrão RFC 7807 (Problem Details) via `ExceptionMapper` global JAX-RS:
- `400 Bad Request`: Falhas de validação sintática (descrição nula, em branco, > 120 caracteres, tipo de mídia/payload inválido).
- `404 Not Found`: Tentativa de alterar ou excluir UUID de categoria inexistente.
- `409 Conflict`: Tentativa de cadastrar ou alterar descrição para um nome duplicado (RN06) ou tentar excluir categoria associada a um ou mais eventos (RN04 / `ON DELETE RESTRICT`).
- `401 Unauthorized` / `403 Forbidden`: Ausência de token JWT válido ou token sem a role `ROLE_ADMIN` nas operações de escrita.

### Rationale
Atende ao **Princípio II da Constituição**, **FR-005** e à **Seção 8 (Padrão de Erro RFC 7807)** da especificação do microservice catalog.

---

## 5. Segurança & Autorização (Keycloak JWT RBAC)

### Decision
Usar `quarkus-oidc` em modo `bearer-only`:
- `GET /api/v1/event-categories`: Leitura pública (`permit`).
- `POST /api/v1/event-categories`: Protegido com `@RolesAllowed("ROLE_ADMIN")`.
- `PUT /api/v1/event-categories/{id}`: Protegido com `@RolesAllowed("ROLE_ADMIN")`.
- `DELETE /api/v1/event-categories/{id}`: Protegido com `@RolesAllowed("ROLE_ADMIN")`.

### Rationale
Garante a matriz de autorização definida na Seção 15.4 da Arquitetura de Solução e atende aos Edge Cases de acesso não autorizado da especificação US-CAT-09.
