# Feature Specification: Ciclo de Vida do Evento (Rascunho e Publicação)

**Feature Branch**: `013-ciclo-vida-evento-rascunho-publicacao`  
**Created**: 2026-07-25  
**Status**: Draft  
**Input**: User description: "* **US-CAT-13 (nova):** Como administrador, quero cadastrar um evento em rascunho e publicá-lo apenas quando estiver pronto, para evitar exibir eventos incompletos no catálogo público. respect: docs\\spec\\microservice-catalog.spec.md, docs\\arch\\arquitetura-solucao.md"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Cadastrar Evento Inicialmente como Rascunho (`DRAFT`) (Priority: P1)

Como um Administrador do sistema, quero cadastrar um evento novo de forma que ele seja gravado com o status inicial `DRAFT` (Rascunho), para poder preparar o conteúdo sem o risco de exibi-lo prematuramente para os clientes no catálogo público de vendas.

**Why this priority**: Soluciona a limitação do sistema legado, onde qualquer evento criado pelo administrador aparecia imediatamente no catálogo público mesmo se estivesse incompleto ou sem imagem promocional.

**Independent Test**: Pode ser testado criando um evento via API administrativa POST e confirmando que ele é gravado no banco de dados com `status = 'DRAFT'`, e que requisições na API pública de consulta de catálogo (`US-CAT-01`) não retornam o evento em rascunho.

**Acceptance Scenarios**:

1. **Given** um Administrador autenticado com a role `ROLE_ADMIN`, **When** ele envia uma solicitação de cadastro de evento, **Then** o sistema deve salvar o evento obrigatoriamente no status `DRAFT`, com `published_at = null`, e retornar HTTP 201 (Created).
2. **Given** um evento em estado `DRAFT`, **When** um cliente realiza uma consulta no catálogo público de eventos (`GET /api/v1/events`), **Then** o sistema NÃO DEVE incluir o evento em rascunho nos resultados da busca.

---

### User Story 2 - Publicar Evento no Catálogo Público (`PUBLISHED`) (Priority: P2)

Como um Administrador do sistema, quero revisar e alterar o status de um evento de `DRAFT` para `PUBLISHED`, para torná-lo visível e disponível para agendamento de shows e compra de ingressos pelo público.

**Why this priority**: É o ato intencional de disponibilização comercial do evento. Garante que apenas conteúdos validados e revisados fiquem visíveis no portal público.

**Independent Test**: Pode ser testado acionando o endpoint administrativo de publicação `POST /api/v1/events/{id}/publish` em um evento no estado `DRAFT`, verificando que o status muda para `PUBLISHED`, a data de publicação `published_at` é gravada, o cache Redis público é invalidado/atualizado e o evento passa a ser retornado na consulta pública.

**Acceptance Scenarios**:

1. **Given** um evento em estado `DRAFT` com dados cadastrais e mídias válidas, **When** o Administrador aciona o comando de publicação, **Then** o sistema deve alterar o status do evento para `PUBLISHED`, preencher o campo `published_at` com o timestamp atual, invalidar/atualizar o cache público Redis e retornar HTTP 200 (OK).
2. **Given** um evento em estado `DRAFT` que não atenda aos pré-requisitos de publicação (ex.: falta de item de mídia obrigatório para exibição visual), **When** o Administrador tenta publicá-lo, **Then** o sistema deve recusar a publicação e retornar HTTP 400 (Bad Request) com relatório de inconformidades via RFC 7807 (Problem Details).
3. **Given** um evento já no estado `PUBLISHED`, **When** o Administrador tenta acionar novamente o comando de publicação, **Then** o sistema deve rejeitar a transição inválida de estado e retornar HTTP 400 (Bad Request).

---

### User Story 3 - Arquivar Evento Encerrado (`ARCHIVED`) (Priority: P3)

Como um Administrador do sistema, quero alterar o estado de um evento publicado para `ARCHIVED` (Arquivado), para removê-lo da divulgação pública mantendo seu histórico preservado no sistema.

**Why this priority**: Permite encerrar o ciclo de exibição comercial de atrações passadas ou canceladas sem perder o histórico relacional com vendas anteriores.

**Independent Test**: Pode ser testado acionando o endpoint de arquivamento `POST /api/v1/events/{id}/archive` e confirmando que o evento é desmarcado da listagem pública em tempo real (HTTP 200 OK).

**Acceptance Scenarios**:

1. **Given** um evento no estado `PUBLISHED`, **When** o Administrador solicita o seu arquivamento, **Then** o sistema deve alterar seu estado para `ARCHIVED`, removê-lo do cache público Redis e retornar HTTP 200 (OK).
2. **Given** um evento no estado `ARCHIVED`, **When** um cliente tenta consultá-lo pela API pública por seu ID, **Then** o sistema deve retornar HTTP 404 (Not Found).

---

### User Story 4 - Listar e Filtrar Eventos por Status no Painel Admin (Priority: P4)

Como um Administrador do sistema, quero listar e filtrar eventos por seu status (`DRAFT`, `PUBLISHED`, `ARCHIVED`), para acompanhar quais atrações estão em preparação, quais estão ativas e quais foram arquivadas.

**Why this priority**: Garante a governança e o acompanhamento visual do pipeline de publicação de atrações no painel de administração.

**Independent Test**: Pode ser testado realizando chamadas GET na API administrativa de eventos com parâmetros de filtro `status=DRAFT`, `status=PUBLISHED` ou `status=ARCHIVED`.

**Acceptance Scenarios**:

1. **Given** eventos em diferentes estágios do ciclo de vida, **When** o Administrador consulta a listagem administrativa com o filtro `status=DRAFT`, **Then** o sistema deve retornar apenas os eventos em rascunho.

---

### Edge Cases

- **Tentativa de transição de estado inválida (ex.: DRAFT → ARCHIVED sem passar por PUBLISHED, ou ARCHIVED → PUBLISHED)**: O sistema de controle de estados da aplicação deve validar a máquina de estados e rejeitar transições não autorizadas com HTTP 400 (Bad Request).
- **Consulta direta por ID na API pública para evento DRAFT ou ARCHIVED**: A API pública de detalhamento de eventos (`US-CAT-03`) deve filtrar a consulta obrigatoriamente por `status = 'PUBLISHED'`. Se o evento existir mas não estiver publicado, deve retornar HTTP 404 (Not Found) para o canal público para não expor a existência do rascunho.
- **Invalidação de Cache sob Alta Carga**: A alteração de estado para `PUBLISHED` ou `ARCHIVED` deve invalidar a chave de cache Redis de forma atômica para evitar inconsistência eventual na listagem pública de atrações.
- **Acesso sem role `ROLE_ADMIN` aos endpoints de transição**: Deve retornar HTTP 401 (Unauthorized) ou 403 (Forbidden).

### User Experience Consistency *(mandatory)*

- **Canais**: Endpoints de transição de estado (`/publish`, `/archive`) e visualização de rascunhos são exclusivos da API administrativa (`ROLE_ADMIN`). A API pública enxerga rigorosamente e apenas eventos em estado `PUBLISHED`.
- **Formato de Erros**: Falhas de pré-requisitos de publicação ou transições de estado inválidas são representadas no padrão RFC 7807 (Problem Details).
- **Semântica da Máquina de Estados**: O ciclo de vida oficial do evento segue a sequência rígida: `DRAFT` → `PUBLISHED` → `ARCHIVED`.
- **Identificadores**: Eventos utilizam UUID v4.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema DEVE implementar o ciclo de vida explícito de eventos com os estados: `DRAFT`, `PUBLISHED` e `ARCHIVED`.
- **FR-002**: O sistema DEVE definir o status padrão `DRAFT` e `published_at = null` para todo novo evento cadastrado na aplicação.
- **FR-003**: O sistema DEVE restringir as consultas do catálogo público de vendas (US-CAT-01, US-CAT-03) estritamente a eventos com `status = 'PUBLISHED'`, utilizando o índice de banco `ix_event_status_published` e cache Redis.
- **FR-004**: O sistema DEVE fornecer o caso de uso `PublishEventUseCase` ativado via endpoint administrativo REST para transicionar o evento de `DRAFT` para `PUBLISHED`, validando a presença dos dados cadastrais obrigatórios e registrando o timestamp atual em `published_at`.
- **FR-005**: O sistema DEVE fornecer endpoint administrativo REST para transicionar o evento de `PUBLISHED` para `ARCHIVED`, retirando-o da circulação pública.
- **FR-006**: O sistema DEVE realizar a invalidação ou atualização síncrona do cache Redis do catálogo público imediatamente após qualquer alteração de estado para ou de `PUBLISHED`.
- **FR-007**: O sistema DEVE utilizar o padrão RFC 7807 (Problem Details) para expor exceções de máquina de estado ou descumprimento de pré-requisitos de publicação.
- **FR-008**: O sistema DEVE conter cobertura de testes automatizados obrigatória: testes unitários para a máquina de estados e validações de `PublishEventUseCase`, testes de contrato REST, testes de integração com Testcontainers PostgreSQL e Redis, e teste E2E cobrindo a jornada de criação em rascunho e posterior publicação.

### Key Entities *(include if feature involves data)*

- **Event (Entidade Raiz com Ciclo de Vida)**:
  - `id`: UUID (Chave primária).
  - `name`: String (Obrigatório, Único, 5 a 50 caracteres).
  - `description`: String (Obrigatório, 20 a 1000 caracteres).
  - `eventCategoryId`: UUID (FK para EventCategory, Obrigatório).
  - `mediaItemId`: UUID (FK para MediaItem, Opcional em DRAFT / Obrigatório para Publicação).
  - `status`: String (Enum: `DRAFT`, `PUBLISHED`, `ARCHIVED`, Padrão: `DRAFT`).
  - `createdAt`: TIMESTAMPTZ (Data de criação).
  - `updatedAt`: TIMESTAMPTZ (Data de modificação).
  - `publishedAt`: TIMESTAMPTZ (Data/hora em que passou para PUBLISHED, nulo se DRAFT).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: As requisições de transição de estado (`publish` e `archive`) respondem em tempo P95 <= 150 ms.
- **SC-002**: 100% dos eventos nos estados `DRAFT` ou `ARCHIVED` são mantidos invisíveis e inacessíveis para os endpoints da API pública do catálogo.
- **SC-003**: 100% das tentativas de transição de estado inválidas são bloqueadas com respostas explicativas RFC 7807 (HTTP 400 Bad Request).

### Performance and Resilience Outcomes *(mandatory)*

- **PR-001**: Latência P95 na transição e persistência do ciclo de vida <= 150 ms.
- **PR-002**: Invalidação do cache Redis público concluída em <= 50 ms após a alteração do estado.
- **PR-003**: Taxa de erros não tratados do servidor (5xx) em steady-state < 0,1%.

## Assumptions

- A funcionalidade é construída no microsserviço `microservice-catalog` ajustando a entidade `Event` no PostgreSQL (`catalog_db`) e adicionando a validação em `PublishEventUseCase`.
- A API pública de consulta de catálogo consome unicamente os dados filtrados por `status = 'PUBLISHED'`, mantendo CQRS leve e cache em Redis.
- Autenticação e autorização via Keycloak JWT exigindo a role `ROLE_ADMIN`.
