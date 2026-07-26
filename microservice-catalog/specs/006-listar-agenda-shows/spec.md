# Feature Specification: US-CAT-06 — Listar Agenda de Shows por Evento ou Local

**Feature Branch**: `006-listar-agenda-shows`

**Created**: 2026-07-25

**Status**: Draft

**Input**: User description: "from docs\spec\microservice-catalog.spec.md create spec for US-CAT-06: Listar a agenda de shows vinculados a um determinado evento ou local. respect E:\develop\repos\java-projects\ticket-monster\docs\arch\arquitetura-solucao.md"

**Origem**: `docs/spec/microservice-catalog.spec.md` (US-CAT-06, RN08), `arquitetura-solucao.md` (seções 4, 6.1, 8, 10, 15.4, 25.3) e Constituição da Modernização (Princípios I a V).

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consultar Shows Vinculados a um Evento Específico (Priority: P1)

Como visitante da plataforma TicketMonster interessado em um evento específico (ex.: "Show da Anitta"), quero consultar a lista de Shows cadastrados para esse evento para saber em quais locais (Venues) ele será apresentado.

**Why this priority**: A navegação por evento para descobrir onde haverá apresentações (P1) é a jornada primária de descoberta de locais e datas para um fã ou comprador.

**Independent Test**: Pode ser testado de forma independente realizando uma requisição HTTP GET pública `GET /api/v1/shows?eventId={id}` com um ID de evento válido e verificando que a resposta 200 OK expõe apenas os Shows vinculados àquele evento com detalhes do local e do evento.

**Acceptance Scenarios**:

1. **Given** um evento publicado que possui 2 Shows agendados (um no "Teatro Municipal" e outro no "Espaço das Américas"), **When** o visitante consulta `GET /api/v1/shows?eventId={eventId}`, **Then** o sistema responde 200 OK exibindo a lista com os 2 Shows e as informações dos respectivos Venues (RN08).
2. **Given** um evento válido que no momento não possui nenhum Show cadastrado, **When** o visitante realiza a busca, **Then** o sistema responde 200 OK com array vazio (`[]`) e os metadados de paginação Zerados (`totalElements: 0`, `totalPages: 0`).
3. **Given** que o visitante passa um `eventId` com formato sintaticamente malformado (ex.: `invalid-uuid`), **When** a requisição atinge o serviço, **Then** o sistema responde HTTP 400 Bad Request com estrutura RFC 7807 Problem Details.

---

### User Story 2 - Consultar Shows Agendados em um Local (Venue) Específico (Priority: P2)

Como visitante interessado na programação de um local de espetáculo (ex.: "Teatro Municipal"), quero consultar todos os Shows agendados para esse local para decidir qual evento assistir naquela arena.

**Why this priority**: A navegação regional/local por arena (P2) permite ao usuário descobrir novos eventos perto de onde mora ou frequenta.

**Independent Test**: Pode ser testado realizando chamada `GET /api/v1/shows?venueId={venueId}` com um ID de local válido e validando que todos os Shows retornados acontecem no local especificado.

**Acceptance Scenarios**:

1. **Given** um Venue com 3 Shows cadastrados para eventos diferentes, **When** o visitante consulta `GET /api/v1/shows?venueId={venueId}`, **Then** o sistema responde 200 OK listando os 3 Shows com os detalhes dos eventos correspondentes.
2. **Given** um Venue válido que não possui Shows agendados, **When** a busca é executada, **Then** o sistema responde 200 OK com lista vazia (`[]`) e metadados de paginação corretos.
3. **Given** que o visitante passa um `venueId` malformado, **When** a requisição atinge a API, **Then** o sistema responde HTTP 400 Bad Request no formato RFC 7807.

---

### User Story 3 - Paginação e Combinação de Filtros na Agenda de Shows (Priority: P3)

Como visitante navegando pela agenda completa de shows, quero poder combinar paginação e ordenação padrão para explorar a programação de forma fluida.

**Why this priority**: Garante UX consistente (P3) e suporte a paginação `page`/`size` (base 0) conforme o Princípio IV da Constituição.

**Independent Test**: Pode ser testado enviando requisições com `page` e `size` para listagens filtradas ou gerais de shows.

**Acceptance Scenarios**:

1. **Given** 15 Shows cadastrados para um evento, **When** o visitante solicita `page=0` e `size=5`, **Then** a resposta 200 OK traz os 5 primeiros Shows e `totalElements: 15`, `totalPages: 3`.
2. **Given** uma requisição com `size=150` (acima do limite 100), **When** processada pela API, **Then** o sistema rejeita a chamada com HTTP 400 Bad Request no formato RFC 7807 Problem Details.

---

### Edge Cases

- **Show Sem Performances**: Se um Show foi criado no cadastro administrativo unindo um Evento e um Venue (RN08), mas ainda não possui sessões (`Performance`) cadastradas, o Show continua sendo listado com `"performanceCount": 0`.
- **Filtros Simultâneos**: Se `eventId` e `venueId` forem informados simultaneamente (`GET /api/v1/shows?eventId={eId}&venueId={vId}`), o sistema aplica a interseção exata (retorna o Show único que associa aquele evento àquele venue específico, ou vazio se não houver associação - RN08).
- **IDs Inexistentes**: Se o UUID informado para `eventId` ou `venueId` for sintaticamente válido mas não existir no banco de dados, o sistema responde 400 Bad Request com Problem Details indicando entidade não encontrada.
- **Falha de Cache**: Em caso de indisponibilidade temporária do Redis, as buscas realizam fallback transparente para a base PostgreSQL (`catalog_db`), utilizando os índices `ix_show_venue` e `uq_show_event_venue`.

---

### User Experience Consistency *(mandatory)*

- **Canal Público Sem Autenticação**: O endpoint `GET /api/v1/shows` É público e NÃO exige token Bearer JWT (`arquitetura-solucao.md`, seção 15.4).
- **Formato de Erro Padronizado**: Respostas de erro 400 (Bad Request) DEVEM utilizar a estrutura RFC 7807 Problem Details.
- **Convenção de Paginação**: Paginação DEVE utilizar os parâmetros `page` (base 0) e `size` (padrão 20, máximo 100).
- **Associação Única de Show**: Cada item retornado representa uma associação única entre um `Event` e um `Venue` (RN08).
- **Nomenclatura DTO**: Propriedades em JSON DEVEM seguir a convenção camelCase (`eventId`, `eventName`, `venueId`, `venueName`, `performanceCount`).

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema MUST disponibilizar o endpoint público `GET /api/v1/shows` para consultar a lista de agenda de Shows.
- **FR-002**: O sistema MUST suportar o parâmetro query `eventId` (UUID) para filtrar os Shows vinculados a um evento específico.
- **FR-003**: O sistema MUST suportar o parâmetro query `venueId` (UUID) para filtrar os Shows vinculados a um local de espetáculo específico.
- **FR-004**: O sistema MUST retornar para cada Show o seu ID (UUID), o objeto do evento vinculado (`id`, `name`, `category`), o objeto do venue vinculado (`id`, `name`, `city`, `state`) e a contagem de performances agendadas (`performanceCount`).
- **FR-005**: O sistema MUST garantir que a associação entre um `Event` e um `Venue` em um Show seja única (RN08).
- **FR-006**: O sistema MUST responder HTTP 400 Bad Request no formato RFC 7807 Problem Details quando `eventId` ou `venueId` forem sintaticamente malformados ou quando indicarem entidades inexistentes.
- **FR-007**: O sistema MUST responder HTTP 200 OK com array vazio (`[]`) e metadados de paginação zerados quando o filtro válido não encontrar Shows cadastrados.
- **FR-008**: O sistema MUST suportar paginação `page` (base 0) e `size` (padrão 20, máximo 100) na listagem de Shows.
- **FR-009**: O sistema MUST aceitar chamadas ao endpoint de agenda de Shows sem exigir token Bearer JWT (`ROLE_CUSTOMER` ou `ROLE_ADMIN`).
- **FR-010**: O sistema MUST possuir suíte automatizada de testes cobrindo:
  - Teste unitário para regras de consulta e filtros por evento/venue.
  - Teste de contrato REST para o schema dos Shows e erros 400 RFC 7807.
  - Teste de integração via Testcontainers para PostgreSQL (`catalog_db`) e Redis.
  - Teste E2E cobrindo a jornada de consulta de agenda de shows.
- **FR-011**: O sistema MUST atender ao orçamento de desempenho (p95 <= 250 ms sob carga com Redis Cache-Aside).

---

### Key Entities *(include if feature involves data)*

- **Show (Espetáculo Agendado)**: Entidade de associação de catálogo que possui ID (UUID), referência a `Event` (evento artístico) e referência a `Venue` (local físico).
- **ShowDTO**: Objeto de transferência de dados que sumariza a associação do Show, contendo os metadados do Evento, os metadados do Venue e a quantidade de sessões agendadas.
- **ShowPage**: Value Object que contem a lista de ShowDTOs e os metadados de paginação (`page`, `size`, `totalElements`, `totalPages`).

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% dos Shows retornados em buscas por `eventId` pertencem exclusivamente ao evento informado.
- **SC-002**: 100% dos Shows retornados em buscas por `venueId` pertencem exclusivamente ao venue informado.
- **SC-003**: 100% das requisições com parâmetros de filtro malformados ou inexistentes resultam em respostas RFC 7807 400 Bad Request.
- **SC-004**: Zero duplicidades na exibição de associações entre o mesmo Evento e o mesmo Venue (RN08).

---

### Performance and Resilience Outcomes *(mandatory)*

- **PR-001**: A latência p95 para requisições de agenda de shows DEVE ser <= 250 ms sob o perfil de carga definido (utilizando cache-aside no Redis nas chaves `catalog:shows:event:{eventId}` e `catalog:shows:venue:{venueId}`).
- **PR-002**: O serviço DEVE sustentar a vazão de pico esperada sem ultrapassar a taxa de erro de 1% no servidor (5xx).
- **PR-003**: Em caso de falha do Redis, a aplicação DEVE realizar fallback transparente de consulta diretamente no PostgreSQL (`catalog_db`) utilizando os índices `ix_show_venue` e `uq_show_event_venue`.

---

## Assumptions

- O `microservice-catalog` gerencia a tabela `catalog.show` no banco PostgreSQL `catalog_db`.
- A associação entre Evento e Venue é única por linha de tabela (`CONSTRAINT uq_show_event_venue UNIQUE (event_id, venue_id)` - RN08).
- A estratégia de cache no Redis armazena os agendamentos de shows em chaves indexadas por evento e por venue com TTL de 1 hora.
- O endpoint `GET /api/v1/shows` é público e livre de autenticação (`arquitetura-solucao.md`, seção 15.4).

---

## Rastreabilidade

| Item desta spec | Origem | Observação |
|---|---|---|
| FR-001 a FR-005 | `microservice-catalog.spec.md` US-CAT-06, RN08 | Consulta de agenda de shows e associação única |
| FR-006, FR-007 | `arquitetura-solucao.md` seção 8 | Respostas de erro RFC 7807 e lista vazia |
| FR-008 | `arquitetura-solucao.md` seção 25.3 (RN42) | Paginação `page`/`size` base 0 |
| FR-009 | `arquitetura-solucao.md` seção 15.4 | Endpoint público `GET /shows` |
| FR-010, FR-011 | Constituição da Modernização, Princípios III e V | Qualidade de testes e latência p95 <= 250ms |
