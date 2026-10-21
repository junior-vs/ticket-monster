# Feature Specification: US-CAT-03 a US-CAT-12 — Detalhes, Locais, Agendas e Gestão Administrativa do Catálogo

**Feature Branch**: `003-catalog-management-api`

**Created**: 2026-07-25

**Status**: Draft

**Input**: User description: "from docs\spec\microservice-catalog.spec.md create spec for US-CAT-03 through US-CAT-12 respect E:\develop\repos\java-projects\ticket-monster\docs\arch\arquitetura-solucao.md"

**Origem**: `docs/spec/microservice-catalog.spec.md` (US-CAT-03 a US-CAT-12, RN01 a RN37), `arquitetura-solucao.md` (seções 4, 6.1, 8, 10, 15.4, 25.3) e Constituição da Modernização (Princípios I a V).

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consulta Pública de Detalhes de Eventos, Locais (Venues) e Agendas de Shows (Priority: P1)

Como visitante da plataforma TicketMonster, quero consultar os detalhes completos de um evento específico por ID, listar os locais de espetáculo (Venues) com suas seções físicas e consultar a agenda de apresentações (Performances) de um show para planejar minha compra de ingressos.

**Why this priority**: Esta jornada representa a camada de leitura pública essencial (P1) que permite a tomada de decisão do comprador antes de iniciar a alocação de assentos ou reserva de ingressos.

**Independent Test**: Pode ser testado de forma independente realizando chamadas HTTP GET públicas aos endpoints de eventos (`/api/v1/events/{id}`), locais (`/api/v1/venues`, `/api/v1/venues/{id}`), shows (`/api/v1/shows?eventId={id}`) e performances (`/api/v1/shows/{id}/performances`), validando o schema JSON e o comportamento de fallback de imagem.

**Acceptance Scenarios**:

1. **Given** um evento com status `PUBLISHED` e ID válido, **When** o visitante consulta `GET /api/v1/events/{id}`, **Then** o sistema retorna 200 OK com o nome, descrição, categoria, detalhes de mídia promocional e data de publicação do evento.
2. **Given** um evento publicado cuja imagem de mídia falhou no carregamento ou possui `fallback_applied = true`, **When** o visitante consulta os detalhes do evento, **Then** o sistema injeta síncronamente a imagem local de fallback (`not_available.jpg`) na resposta (RN35).
3. **Given** a consulta de locais de espetáculo `GET /api/v1/venues`, **When** o visitante solicita a lista de venues, **Then** o sistema retorna 200 OK exibindo todos os locais cadastrados com nome, endereço e descrição.
4. **Given** um ID de venue válido, **When** o visitante consulta `GET /api/v1/venues/{id}`, **Then** o sistema retorna 200 OK detalhando o local e a lista de suas seções físicas com a capacidade calculada (`rows * row_capacity`) de cada seção (RN12).
5. **Given** um show cadastrado unindo um evento e um venue, **When** o visitante consulta `GET /api/v1/shows/{showId}/performances`, **Then** o sistema retorna 200 OK exibindo a lista de sessões (performances) com data/hora ordenadas cronologicamente (RN09).

---

### User Story 2 - Gestão Administrativa do Catálogo de Eventos e Categorias (Priority: P2)

Como administrador do sistema, quero cadastrar, alterar e gerenciar o ciclo de vida de eventos (`DRAFT` → `PUBLISHED` → `ARCHIVED`) e categorias de eventos com controle de acesso RBAC (`ROLE_ADMIN`), garantindo que apenas informações válidas sejam expostas ao público.

**Why this priority**: Permite que a equipe de operações alimente e gerencie o catálogo de produtos (P2), impondo restrições de integridade e segurança de acesso.

**Independent Test**: Pode ser testado autenticando como `ROLE_ADMIN` e executando chamadas POST/PUT/DELETE em `/api/v1/events` e `/api/v1/event-categories`, validando as constraints de tamanho, unicidade e regras de autorização.

**Acceptance Scenarios**:

1. **Given** um usuário autenticado com a role `ROLE_ADMIN`, **When** envia requisição POST para criar evento com nome válido (5-50 chars), descrição válida (20-1000 chars) e categoria ativa, **Then** o evento é criado com status inicial `DRAFT` e retorna 201 Created.
2. **Given** um evento em estado `DRAFT`, **When** o administrador altera seu status para `PUBLISHED`, **Then** o sistema preenche `published_at` com o instante da transição e o evento passa a figurar nas buscas públicas.
3. **Given** um administrador tentando cadastrar um evento com nome já existente no sistema, **When** submete a criação, **Then** o sistema rejeita com HTTP 409 Conflict (RN01).
4. **Given** um administrador tentando excluir uma categoria (`EventCategory`) que possui eventos associados, **When** solicita a exclusão, **Then** o sistema bloqueia a operação (ON DELETE RESTRICT) e retorna HTTP 409 Conflict.
5. **Given** uma requisição administrativa de evento ou categoria enviada por um visitante sem token ou sem a role `ROLE_ADMIN`, **When** a requisição atinge o serviço, **Then** o sistema rejeita com HTTP 401 Unauthorized (sem token) ou 403 Forbidden (role inadequada).

---

### User Story 3 - Gestão Administrativa de Venues, Seções e Agendamento de Shows/Performances (Priority: P3)

Como administrador do sistema, quero cadastrar os locais físicos (Venues), suas seções estruturais e agendar Shows e Performances temporais sem permitir duplicações de datas ou conflitos relacionais.

**Why this priority**: Garante a montagem da estrutura física e da agenda de espetáculos (P3) necessárias para a venda e reserva de assentos no microsserviço de inventário.

**Independent Test**: Pode ser testado isoladamente criando Venues, adicionando seções e agendando Performances para um Show, verificando as travas de unicidade de seção por venue e unicidade de data/hora por show.

**Acceptance Scenarios**:

1. **Given** um administrador criando uma seção física em um Venue com 10 fileiras e 15 assentos por fileira, **When** salva a seção, **Then** a capacidade total é automaticamente calculada e armazenada como 150 assentos (`GENERATED ALWAYS AS (number_of_rows * row_capacity)`) (RN12).
2. **Given** um Venue "Teatro Municipal", **When** o administrador tenta adicionar duas seções com o mesmo nome "Platéia VIP" no mesmo Venue, **Then** o sistema rejeita com HTTP 409 Conflict (RN11).
3. **Given** um Show associando um evento a um venue, **When** o administrador tenta agendar duas performances do mesmo Show na mesma data e hora exatas, **Then** o sistema rejeita com HTTP 409 Conflict (RN10).
4. **Given** um agendamento de Show, **When** o administrador tenta vincular novamente o mesmo `Event` e `Venue`, **Then** o sistema bloqueia com HTTP 409 Conflict devido à regra de unicidade de associação (RN08).

---

### User Story 4 - Cadastro e Validação de Mídia Promocional Extensível (Priority: P4)

Como administrador do sistema, quero cadastrar itens de mídia promocional (imagens, vídeos, áudios) com validação de URL e fallback síncrono para ilustrar os eventos do catálogo.

**Why this priority**: Garante a rica apresentação visual e multimídia do catálogo (P4), mantendo a resiliência contra links quebrados ou indisponíveis.

**Independent Test**: Pode ser testado enviando chamadas POST para `/api/v1/media-items` com URLs válidas (`http`/`https`), validando a unicidade de URL e o catálogo extensível de tipos de mídia (`IMAGE`, `VIDEO`, `AUDIO`).

**Acceptance Scenarios**:

1. **Given** uma URL promocional válida (`https://midia.ticketmonster.com/banner.jpg`) e o tipo `IMAGE`, **When** o administrador cadastra o item de mídia, **Then** o sistema valida o esquema `http/https`, verifica a unicidade e persiste com sucesso (RN34 alterada, RN37).
2. **Given** um administrador tentando cadastrar um item de mídia com URL duplicada já existente, **When** envia a requisição, **Then** o sistema rejeita com HTTP 409 Conflict (RN37).
3. **Given** um item de mídia cujo carregamento remoto falha síncronamente na resolução HTTP, **When** o item é consultado ou atribuído a um evento, **Then** o sistema marca `fallback_applied = true` e substitui o arquivo pelo fallback padrão local (`not_available.jpg`) sem abortar o fluxo (RN35, CA-CAT-03-MED).

---

### Edge Cases

- **Exclusão de Entidades com Relacionamentos Ativos**: A exclusão de um `Event` em DRAFT exclui em cascata seus Shows e Performances (`ON DELETE CASCADE`), mas a exclusão de uma `EventCategory` com eventos associados ou de um `Venue` com shows ativos é bloqueada (`ON DELETE RESTRICT` -> 409 Conflict).
- **Mídia Não Informada**: O vínculo de mídia em um evento é opcional (`media_item_id NULL`). Quando nulo, o sistema exibe o fallback padrão no frontend sem falhas de carregamento (RN05, RN35).
- **Concorrência em Cadastro de Data/Hora**: Se duas requisições administrativas tentarem agendar exatamente a mesma performance para o mesmo show concorrentemente, o índice único de banco `uq_performance_show_date` garante que apenas uma prevalecerá e a outra receberá 409 Conflict.
- **Validação de Schema de URL**: URLs que não comecem com `http://` ou `https://` são rejeitadas pelo banco (`ck_media_item_url_scheme`) e pela camada de validação da API com HTTP 400 Problem Details.

---

### User Experience Consistency *(mandatory)*

- **Separação de Endpoints Públicos e Administrativos**: Endpoints de leitura de catálogo (`GET /events/{id}`, `/venues`, `/shows`, `/performances`) DEVEM ser públicos (sem token). Endpoints de alteração (POST/PUT/DELETE) DEVEM exigir JWT com `ROLE_ADMIN`.
- **Formato de Erro Padronizado**: Todos os erros de validação (400), autenticação (401), autorização (403), não encontrado (404) e conflito (409) DEVEM retornar JSON no formato RFC 7807 Problem Details (`arquitetura-solucao.md`, seção 8).
- **Datas e Timestamps**: Todos os campos temporais (`performance_date`, `published_at`, `created_at`) DEVEM ser serializados em UTC no formato ISO 8601 (`timestamptz`).
- **Capacidade de Seção Transparente**: As seções de um Venue DEVEM expor o atributo `capacity` já calculado (`number_of_rows * row_capacity`) em todas as respostas de leitura.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema MUST disponibilizar endpoint público `GET /api/v1/events/{id}` retornando os detalhes do evento publicado correspondente ao ID informado.
- **FR-002**: O sistema MUST injetar o caminho para a imagem de fallback local (`not_available.jpg`) quando o evento consultado não possuir mídia ou quando a resolução de mídia registrar falha (`fallback_applied = true`) (RN35).
- **FR-003**: O sistema MUST disponibilizar endpoint público `GET /api/v1/venues` para listar todos os locais de espetáculo cadastrados.
- **FR-004**: O sistema MUST disponibilizar endpoint público `GET /api/v1/venues/{id}` exibindo os detalhes do local, seu endereço e a lista de seções físicas com a capacidade total calculada por seção (`capacity = number_of_rows * row_capacity`) (RN12).
- **FR-005**: O sistema MUST disponibilizar endpoint público `GET /api/v1/shows` permitindo filtrar agendamentos de shows por `eventId` ou `venueId` (RN08).
- **FR-006**: O sistema MUST disponibilizar endpoint público `GET /api/v1/shows/{showId}/performances` retornando a lista de sessões/performances agendadas para o show, ordenadas cronologicamente por `performance_date` (RN09).
- **FR-007**: O sistema MUST exigir autenticação JWT contendo a role `ROLE_ADMIN` para todas as operações de modificação no catálogo (`POST`, `PUT`, `DELETE` em `/events`, `/event-categories`, `/venues`, `/shows`, `/media-items`).
- **FR-008**: O sistema MUST retornar HTTP 401 Unauthorized para acessos sem token e HTTP 403 Forbidden para usuários sem a role `ROLE_ADMIN` em rotas administrativas.
- **FR-009**: O sistema MUST validar as regras de criação/edição de evento: nome único (RN01), tamanho do nome entre 5 e 50 chars (RN02), tamanho da descrição entre 20 e 1000 chars (RN03) e associação obrigatória com `EventCategory` existente (RN04).
- **FR-010**: O sistema MUST gerenciar o ciclo de vida do evento através do atributo `status` (`DRAFT`, `PUBLISHED`, `ARCHIVED`), garantindo que apenas eventos em `PUBLISHED` sejam retornados nas consultas públicas.
- **FR-011**: O sistema MUST preencher automaticamente o campo `published_at` no instante em que o status do evento é alterado para `PUBLISHED`.
- **FR-012**: O sistema MUST validar as regras de categoria de evento: descrição única e não nula (RN06).
- **FR-013**: O sistema MUST impedir a exclusão de uma `EventCategory` que possua eventos vinculados (`ON DELETE RESTRICT`), retornando HTTP 409 Conflict.
- **FR-014**: O sistema MUST validar as regras de Venue: nome único e não vazio (RN07).
- **FR-015**: O sistema MUST validar as regras de seção física: nome da seção único dentro do mesmo Venue (RN11) e valores de fileira/capacidade obrigatoriamente estritamente positivos.
- **FR-016**: O sistema MUST validar a regra de associação de Show: exatamente um vinculo único entre um `Event` e um `Venue` (`uq_show_event_venue`) (RN08), retornando HTTP 409 Conflict em duplicidades.
- **FR-017**: O sistema MUST validar as regras de Performance: data/hora obrigatória (RN09) e unicidade de data/hora por Show (RN10), retornando HTTP 409 Conflict se duas performances do mesmo show forem agendadas para o mesmo instante.
- **FR-018**: O sistema MUST validar itens de mídia promocional: URL estruturalmente válida (`http://` ou `https://`), única na base de dados (RN37) e tipo pertencente ao catálogo extensível de mídia (`media_type_catalog`: `IMAGE`, `VIDEO`, `AUDIO`) (RN34 alterada).
- **FR-019**: O sistema MUST responder todas as falhas de validação, erros relacionais e restrições de segurança utilizando o padrão RFC 7807 Problem Details.
- **FR-020**: O sistema MUST possuir suíte automatizada de testes cobrindo:
  - Testes unitários para regras de domínio e cálculos de capacidade.
  - Testes de contrato REST para todos os schemas de DTOs e Problem Details.
  - Testes de integração via Testcontainers para a base `catalog_db` e cache Redis.
  - Teste end-to-end (E2E) para as jornadas críticas de consulta pública e cadastro de eventos.
- **FR-021**: O sistema MUST atender aos orçamentos de desempenho definidos para leitura pública de catálogo (p95 <= 250 ms sob carga com Redis Cache-Aside).

---

### Key Entities *(include if feature involves data)*

- **Event (Evento)**: Agregado raiz que contém ID (UUID), nome único, descrição, referência a `EventCategory`, referência opcional a `MediaItem`, status (`DRAFT`, `PUBLISHED`, `ARCHIVED`), timestamps (`created_at`, `updated_at`, `published_at`).
- **EventCategory (Categoria de Evento)**: Entidade de domínio que possui ID (UUID) e descrição única.
- **Venue (Local de Espetáculo)**: Agregado raiz que contém ID (UUID), nome único, descrição, endereço (`address_line`, `city`, `state`, `postal_code`, `country`) e coleção de seções físicas.
- **Section (Seção Física)**: Entidade pertencente a Venue que contém ID (UUID), nome da seção, quantidade de fileiras, capacidade da fileira e capacidade total armazenada em coluna gerada (`capacity`).
- **Show (Espetáculo Agendado)**: Entidade de associação contendo ID (UUID), referência a `Event` e referência a `Venue`.
- **Performance (Sessão de Show)**: Entidade contendo ID (UUID), referência a `Show`, data/hora da apresentação (`performance_date` em `timestamptz`) e descrição opcional.
- **MediaItem (Item de Mídia)**: Entidade contendo ID (UUID), código do tipo de mídia (`media_type_code`), URL única, nome de arquivo em cache e indicador de fallback aplicado.
- **MediaTypeCatalog (Catálogo de Tipos de Mídia)**: Tabela de domínio contendo código (`IMAGE`, `VIDEO`, `AUDIO`), descrição e status ativado.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% dos eventos exibidos nas consultas públicas (`GET /api/v1/events/{id}`) possuem status `PUBLISHED`.
- **SC-002**: 100% das falhas de carregamento de mídia remota resultam na exibição da imagem de fallback local (`not_available.jpg`) sem provocar indisponibilidade da API.
- **SC-003**: 100% das tentativas de violação de unicidade (nome de evento, nome de venue, seção duplicada por venue, data de performance duplicada por show) são rejeitadas com código HTTP 409 Conflict e payload Problem Details.
- **SC-004**: 100% das requisições administrativas sem credenciais de `ROLE_ADMIN` são bloqueadas com status HTTP 401 Unauthorized ou 403 Forbidden.

---

### Performance and Resilience Outcomes *(mandatory)*

- **PR-001**: A latência p95 para requisições públicas de detalhes de eventos, venues e agendas de shows DEVE ser <= 250 ms sob o perfil de carga acordado, utilizando a estratégia de cache-aside no Redis (`catalog:event:{id}`, `catalog:shows:performance:{id}`).
- **PR-002**: O serviço DEVE sustentar a vazão de pico esperada em navegações de catálogo sem ultrapassar a taxa de erro de 1% no servidor (5xx).
- **PR-003**: Em caso de queda do Redis, o `microservice-catalog` DEVE executar fallback de leitura direto na base PostgreSQL (`catalog_db`), preservando a funcionalidade e os contratos de resposta sem gerar erro 500.

---

## Assumptions

- O `microservice-catalog` gerencia e possui autoridade exclusiva sobre o schema relacional `catalog` no banco PostgreSQL `catalog_db`.
- Operações de leitura de catálogo público utilizam Redis Cache-Aside (`catalog:event:{id}`, `catalog:venues`, `catalog:shows:{id}`) com TTL de 1 hora, invalidado por eventos de atualização ou publicação de catálogo (`arquitetura-solucao.md`, seção 10).
- As permissões de acesso são validadas em cada microsserviço através da revalidação do token JWT repassado pelo API Gateway (*token relay*) e checagem da claim `realm_access.roles` (`ROLE_ADMIN`) (`arquitetura-solucao.md`, seção 15.3 e 15.4).
- Respostas de erro utilizam estritamente o formato RFC 7807 Problem Details, conforme especificado na arquitetura de referência e na Constituição.

---

## Rastreabilidade

| Item desta spec | Origem | Observação |
|---|---|---|
| FR-001, FR-002 | `microservice-catalog.spec.md` US-CAT-03, RN35 | Detalhes do evento e fallback de mídia |
| FR-003, FR-004 | `microservice-catalog.spec.md` US-CAT-04, US-CAT-05, RN12 | Lista e detalhes de Venues com seções geradas |
| FR-005, FR-006 | `microservice-catalog.spec.md` US-CAT-06, US-CAT-07, RN09 | Agendas de shows e sessões (performances) |
| FR-007, FR-008 | `arquitetura-solucao.md` seção 15.4 (matriz RBAC) | Controle de acesso administrativo via `ROLE_ADMIN` |
| FR-009, FR-010, FR-011 | `microservice-catalog.spec.md` US-CAT-08, RN01-RN04, `[NOVO]` | Gestão de eventos e ciclo de vida |
| FR-012, FR-013 | `microservice-catalog.spec.md` US-CAT-09, RN06, `[NOVO]` | Categoria de evento e restrição de exclusão |
| FR-014, FR-015 | `microservice-catalog.spec.md` US-CAT-10, RN07, RN11, RN12 | Cadastro de venues e seções físicas |
| FR-016, FR-017 | `microservice-catalog.spec.md` US-CAT-11, RN08, RN09, RN10 | Agendamento de shows e unicidade de performances |
| FR-018 | `microservice-catalog.spec.md` US-CAT-12, RN34 (alterada), RN37 | Cadastro de mídia e catálogo extensível |
| FR-019 | `arquitetura-solucao.md` seção 8 | Resposta de erro RFC 7807 Problem Details |
| FR-020, FR-021 | Constituição da Modernização, Princípios III e V | Qualidade de testes e orçamentos de desempenho |
