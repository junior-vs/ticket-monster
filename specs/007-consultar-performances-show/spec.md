# Feature Specification: US-CAT-07 — Consultar Sessões (Performances) Ativas de um Show para Compra

**Feature Branch**: `007-consultar-performances-show`

**Created**: 2026-07-25

**Status**: Draft

**Input**: User description: "from docs\spec\microservice-catalog.spec.md create spec for US-CAT-07: Consultar sessões (performances) ativas de um show para compra.. respect E:\develop\repos\java-projects\ticket-monster\docs\arch\arquitetura-solucao.md"

**Origem**: `docs/spec/microservice-catalog.spec.md` (US-CAT-07, RN09, RN10), `arquitetura-solucao.md` (seções 4, 6.1, 8, 10, 15.4, 25.3) e Constituição da Modernização (Princípios I a V).

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consultar Sessões (Performances) de um Show por ID (Priority: P1)

Como comprador da plataforma TicketMonster, quero consultar as sessões (performances) ativas agendadas para um determinado Show para escolher a data e horário em que desejo assistir ao espetáculo e prosseguir para a alocação de ingressos.

**Why this priority**: A seleção da sessão/data (P1) é o passo divisor de águas entre a navegação no catálogo e o início do processo transacional de compra de ingressos no serviço de inventário.

**Independent Test**: Pode ser testado de forma independente realizando uma requisição HTTP GET pública `GET /api/v1/shows/{showId}/performances` com um ID de Show válido e verificando que a resposta 200 OK expõe as performances agendadas com data/hora em ISO 8601 (`timestamptz`), descrição da sessão e ID de cada performance.

**Acceptance Scenarios**:

1. **Given** um Show cadastrado com ID válido contendo 3 sessões agendadas ("25/12/2026 20:00", "26/12/2026 17:00", "26/12/2026 21:00"), **When** o comprador consulta `GET /api/v1/shows/{showId}/performances`, **Then** o sistema responde 200 OK listando as 3 sessões ordenadas cronologicamente pela data/hora da apresentação (`performance_date` ASC) (RN09).
2. **Given** um Show com ID válido que no momento não possui nenhuma performance cadastrada, **When** o comprador realiza a consulta, **Then** o sistema responde 200 OK com um array vazio (`[]`) e metadados de paginação indicando `totalElements: 0` e `totalPages: 0`.
3. **Given** que o comprador informa um `showId` inexistente no banco relacional, **When** a requisição é processada, **Then** o sistema responde HTTP 404 Not Found com estrutura RFC 7807 Problem Details.
4. **Given** que o comprador informa um `showId` sintaticamente malformado (ex.: `abc-123`), **When** a requisição atinge a API, **Then** o sistema responde HTTP 400 Bad Request no formato RFC 7807.

---

### User Story 2 - Filtragem de Performances Futuras Elegíveis para Venda (Priority: P2)

Como comprador navegando pela agenda de um Show, quero visualizar apenas as sessões com datas/horários futuros elegíveis para venda, evitando tentar comprar ingressos para apresentações que já ocorreram no passado.

**Why this priority**: Garante que a interface pública exiba apenas sessões operacionais válidas para a jornada de checkout (P2).

**Independent Test**: Pode ser testado validando que sessões com `performance_date` anterior ao momento da consulta são automaticamente omitidas da listagem pública ou filtradas via parâmetro query opcional `upcomingOnly=true`.

**Acceptance Scenarios**:

1. **Given** um Show com sessões no passado e sessões no futuro, **When** o comprador realiza a consulta pública de performances, **Then** apenas as sessões com `performance_date >= agora` são retornadas por padrão para compra.
2. **Given** um Show cujas sessões já foram todas concluídas no passado, **When** a consulta é realizada, **Then** o sistema responde 200 OK com array vazio (`[]`), sinalizando ausência de sessões ativas para venda.

---

### User Story 3 - Paginação e Representação Padronizada de Datas (Priority: P3)

Como aplicativo cliente (web/mobile), quero receber as datas das sessões serializadas em ISO 8601 UTC com suporte a paginação `page`/`size`.

**Why this priority**: Garante a interoperabilidade de fusos horários e conformidade com o Princípio IV da Constituição (P3).

**Independent Test**: Inspecionar a resposta JSON de `GET /api/v1/shows/{showId}/performances` garantindo que o campo `performanceDate` é formatado em ISO 8601 UTC (ex.: `2026-12-25T20:00:00Z`).

**Acceptance Scenarios**:

1. **Given** a resposta de performances de um show, **When** o cliente inspeciona o campo `performanceDate`, **Then** o valor está estritamente formatado em ISO 8601 UTC.
2. **Given** uma requisição de performances com `page=0` e `size=10`, **When** processada, **Then** o sistema retorna no máximo 10 itens e os metadados de paginação preenchidos.

---

### Edge Cases

- **Unicidade de Sessão por Show**: Duas sessões para o mesmo Show não podem ser agendadas exatamente no mesmo instante (`CONSTRAINT uq_performance_show_date UNIQUE (show_id, performance_date)`) (RN10).
- **Show sem Evento ou Venue Ativo**: Se o evento ou venue vinculado ao Show for inativado no cadastro master, a rota de performances do show reflete o estado e retorna 404 Not Found ou lista vazia.
- **Falha de Cache**: Em caso de falha de conexão com o Redis (`catalog:shows:performance:{showId}`), o sistema realiza fallback transparente para o PostgreSQL (`catalog_db`), utilizando o índice `ix_performance_date`.

---

### User Experience Consistency *(mandatory)*

- **Canal Público Sem Autenticação**: O endpoint `GET /api/v1/shows/{showId}/performances` É público e NÃO exige token Bearer JWT (`arquitetura-solucao.md`, seção 15.4).
- **Formato de Erro Padronizado**: Respostas de erro 400 (Bad Request) e 404 (Not Found) DEVEM utilizar a estrutura RFC 7807 Problem Details.
- **Convenção de Paginação**: Paginação DEVE utilizar os parâmetros `page` (base 0) e `size` (padrão 20, máximo 100).
- **Ordenação Cronológica**: As performances DEVEM ser ordenadas de forma ascendente pela data/hora da apresentação (`performance_date` ASC).
- **Formato Temporal**: O atributo `performanceDate` DEVE ser serializado em ISO 8601 UTC (`timestamptz`).

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema MUST disponibilizar o endpoint público `GET /api/v1/shows/{showId}/performances` para consultar as sessões/performances de um Show por seu ID (UUID).
- **FR-002**: O sistema MUST retornar para cada performance o seu identificador único (UUID), o ID do show (`showId`), a data e hora da apresentação (`performanceDate` em ISO 8601 UTC) e a descrição opcional da sessão (RN09).
- **FR-003**: O sistema MUST ordenar por padrão a lista de performances em ordem cronológica ascendente (`performance_date` ASC).
- **FR-004**: O sistema MUST filtrar por padrão apenas performances com data/hora futura ou igual à data atual (`performance_date >= agora`) para exibição pública de compra.
- **FR-005**: O sistema MUST responder HTTP 404 Not Found no formato RFC 7807 Problem Details quando o `showId` informado não existir no cadastro de Shows (`catalog.show`).
- **FR-006**: O sistema MUST responder HTTP 400 Bad Request no formato RFC 7807 Problem Details quando o `showId` for um UUID sintaticamente malformado.
- **FR-007**: O sistema MUST responder HTTP 200 OK com array vazio (`[]`) e metadados de paginação zerados quando o Show for válido mas não possuir performances futuras ativas.
- **FR-008**: O sistema MUST garantir a unicidade de data/hora de sessão para o mesmo Show (`uq_performance_show_date`) (RN10).
- **FR-009**: O sistema MUST suportar paginação `page` (base 0) e `size` (padrão 20, máximo 100) no resultado das performances.
- **FR-010**: O sistema MUST aceitar chamadas ao endpoint de performances sem exigir token Bearer JWT (`ROLE_CUSTOMER` ou `ROLE_ADMIN`).
- **FR-011**: O sistema MUST possuir suíte automatizada de testes cobrindo:
  - Teste unitário para ordenação cronológica e filtragem de performances futuras.
  - Teste de contrato REST para o schema de PerformanceDTO e respostas de erro 400/404 RFC 7807.
  - Teste de integração via Testcontainers para PostgreSQL (`catalog_db`) e Redis.
  - Teste E2E cobrindo a jornada de consulta de sessões para compra.
- **FR-012**: O sistema MUST atender ao orçamento de desempenho (p95 <= 250 ms sob carga com Redis Cache-Aside na chave `catalog:shows:performance:{showId}`).

---

### Key Entities *(include if feature involves data)*

- **Performance (Sessão de Show)**: Entidade de catálogo que possui ID (UUID), referência a `Show` (`show_id`), data/hora da apresentação (`performance_date` em `timestamptz`) e descrição da sessão.
- **PerformanceDTO**: Objeto de transferência de dados que expõe os detalhes da sessão de show para o cliente público.
- **PerformancePage**: Value Object que contem a lista de PerformanceDTOs e os metadados de paginação (`page`, `size`, `totalElements`, `totalPages`).

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% das sessões retornadas na busca pública correspondem a apresentações com `performance_date >= agora` e pertencem exclusivamente ao Show consultado.
- **SC-002**: 100% das respostas de performances são serializadas com datas em formato ISO 8601 UTC ordenadas cronologicamente.
- **SC-003**: 100% das consultas com `showId` inexistente ou malformado resultam em respostas RFC 7807 (404 Not Found ou 400 Bad Request).
- **SC-004**: Zero conflitos de duplicidade de horário de sessão para o mesmo Show (RN10).

---

### Performance and Resilience Outcomes *(mandatory)*

- **PR-001**: A latência p95 para a consulta de performances de um show DEVE ser <= 250 ms sob o perfil de carga definido (utilizando cache-aside no Redis na chave `catalog:shows:performance:{showId}`).
- **PR-002**: O serviço DEVE sustentar a vazão de pico de consultas durante a abertura de vendas de shows populares sem ultrapassar a taxa de erro de 1% no servidor (5xx).
- **PR-003**: Em caso de falha de conexão com o Redis, a aplicação DEVE realizar fallback transparente de consulta direto no PostgreSQL (`catalog_db`) utilizando o índice `ix_performance_date`.

---

## Assumptions

- O `microservice-catalog` gerencia a tabela `catalog.performance` no banco PostgreSQL `catalog_db`.
- O horário da performance é armazenado como `TIMESTAMPTZ` para suportar fusos horários de forma transparente.
- A chave de cache no Redis utiliza o formato `catalog:shows:performance:{showId}` com TTL de 1 hora, invalidada sempre que uma sessão for adicionada ou editada.
- O endpoint `GET /api/v1/shows/{showId}/performances` é público e livre de autenticação JWT (`arquitetura-solucao.md`, seção 15.4).

---

## Rastreabilidade

| Item desta spec | Origem | Observação |
|---|---|---|
| FR-001 a FR-004 | `microservice-catalog.spec.md` US-CAT-07, RN09 | Consulta de performances/sessões de show |
| FR-005, FR-006, FR-007 | `arquitetura-solucao.md` seção 8 | Respostas de erro RFC 7807 (404/400) e lista vazia |
| FR-008 | `microservice-catalog.spec.md` RN10 | Unicidade de sessão por show (`uq_performance_show_date`) |
| FR-009, FR-010 | `arquitetura-solucao.md` seções 15.4 e 25.3 | Paginação base 0 e rota pública |
| FR-011, FR-012 | Constituição da Modernização, Princípios III e V | Qualidade de testes e latência p95 <= 250ms |
